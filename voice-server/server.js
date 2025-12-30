/**
 * RavenClient Voice Chat Server
 * 
 * Central voice server for EU/US VPS deployment
 * Uses raw TCP for control (compatible with Java Socket) and UDP for audio.
 * 
 * Dependencies to install:
 *   npm init -y
 * 
 * Run with:
 *   node server.js
 * 
 * Ports:
 *   - TCP 25567: Control/signaling (line-based JSON)
 *   - UDP 25566: Audio data
 */

const net = require('net');
const dgram = require('dgram');
const crypto = require('crypto');

// Prevent unhandled errors from crashing the server
process.on('uncaughtException', (err) => {
    console.error('[Server] Uncaught exception:', err);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('[Server] Unhandled rejection:', reason);
});

// UUID v4 generator using crypto
function uuidv4() {
    return crypto.randomUUID();
}

// Configuration
const CONFIG = {
    TCP_PORT: 25567,
    UDP_PORT: 25566,
    MAX_ROOM_USERS: 20,
    HEARTBEAT_INTERVAL: 30000,
    SESSION_TIMEOUT: 60000
};

// State
const sessions = new Map();      // sessionId -> { socket, udpAddress, udpPort, userId, roomId, discordName, displayName }
const rooms = new Map();         // roomId -> { name, ownerId, isPrivate, linkedPartyId, maxUsers, userIds: Set }
const users = new Map();         // userId -> { sessionId, discordName, displayName, muted, deafened }

// Discord bot token validation - calls the Discord bot's API
async function validateDiscordToken(token) {
    const http = require('http');
    
    return new Promise((resolve) => {
        const postData = JSON.stringify({ token });
        
        const req = http.request({
            hostname: 'localhost',
            port: 25568,
            path: '/validate',
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(postData)
            },
            timeout: 5000
        }, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const result = JSON.parse(data);
                    resolve(result);
                } catch (err) {
                    resolve({ valid: false, error: 'Invalid response from auth server' });
                }
            });
        });
        
        req.on('error', (err) => {
            console.error('[Auth] Error validating token:', err.message);
            resolve({ valid: false, error: 'Auth server unavailable' });
        });
        
        req.on('timeout', () => {
            req.destroy();
            resolve({ valid: false, error: 'Auth server timeout' });
        });
        
        req.write(postData);
        req.end();
    });
}

// TCP Server (Control/Signaling)
const tcpServer = net.createServer((socket) => {
    const sessionId = uuidv4();
    console.log(`[TCP] New connection: ${sessionId} from ${socket.remoteAddress}`);
    
    // Disable socket timeout - we handle keepalive via heartbeats
    socket.setTimeout(0);
    socket.setKeepAlive(true, 30000);
    
    sessions.set(sessionId, {
        socket,
        udpAddress: null,
        udpPort: null,
        userId: null,
        roomId: null,
        discordName: null,
        displayName: null,
        lastHeartbeat: Date.now(),
        buffer: ''
    });
    
    // Send session ID to client
    sendToSession(sessionId, { type: 'connected', sessionId });
    
    socket.on('data', async (data) => {
        const session = sessions.get(sessionId);
        if (!session) return;
        
        // Update heartbeat on any data received
        session.lastHeartbeat = Date.now();
        
        // Accumulate data in buffer and process complete lines
        session.buffer += data.toString();
        
        let newlineIndex;
        while ((newlineIndex = session.buffer.indexOf('\n')) !== -1) {
            const line = session.buffer.substring(0, newlineIndex).trim();
            session.buffer = session.buffer.substring(newlineIndex + 1);
            
            if (line.length > 0) {
                try {
                    const message = JSON.parse(line);
                    await handleControlMessage(sessionId, message);
                } catch (err) {
                    console.error(`[TCP] Error handling message from ${sessionId}:`, err);
                    sendToSession(sessionId, { type: 'error', message: err.message });
                }
            }
        }
    });
    
    socket.on('close', () => {
        console.log(`[TCP] Connection closed: ${sessionId}`);
        handleDisconnect(sessionId);
    });
    
    socket.on('error', (err) => {
        console.error(`[TCP] Socket error:`, err.message);
    });
});

tcpServer.listen(CONFIG.TCP_PORT, () => {
    console.log(`[TCP] Listening on port ${CONFIG.TCP_PORT}`);
});

async function handleControlMessage(sessionId, message) {
    const session = sessions.get(sessionId);
    if (!session) return;
    
    session.lastHeartbeat = Date.now();
    
    const type = message.type;
    const data = message.data || message; // Support both { type, data } and flat structure
    
    switch (type) {
        case 'auth':
            await handleAuth(sessionId, data);
            break;
            
        case 'join_room':
            handleJoinRoom(sessionId, data);
            break;
            
        case 'create_room':
            handleCreateRoom(sessionId, data);
            break;
            
        case 'leave_room':
            handleLeaveRoom(sessionId);
            break;
            
        case 'mute_state':
            handleMuteState(sessionId, data);
            break;
            
        case 'deafen_state':
            handleDeafenState(sessionId, data);
            break;
            
        case 'list_rooms':
            handleListRooms(sessionId);
            break;
            
        case 'heartbeat':
            sendToSession(sessionId, { type: 'heartbeat_ack' });
            break;
            
        case 'disconnect':
            handleDisconnect(sessionId);
            break;
            
        default:
            console.log(`[TCP] Unknown message type: ${type}`);
    }
}

async function handleAuth(sessionId, data) {
    const session = sessions.get(sessionId);
    const { token, minecraft_name, nickname } = data;
    
    console.log(`[Auth] Auth request from ${minecraft_name} with token: ${token ? token.substring(0, 8) + '...' : 'none'}`);
    
    // Validate Discord token
    const validation = await validateDiscordToken(token);
    
    if (!validation.valid) {
        console.log(`[Auth] Failed: ${validation.error}`);
        sendToSession(sessionId, { type: 'auth_response', success: false, error: validation.error || 'Invalid token' });
        return;
    }
    
    // Store user info
    session.userId = validation.discordId;
    session.discordName = validation.discordName;
    session.displayName = nickname || minecraft_name;
    
    users.set(validation.discordId, {
        sessionId,
        discordName: validation.discordName,
        displayName: session.displayName,
        muted: false,
        deafened: false
    });
    
    sendToSession(sessionId, {
        type: 'auth_response',
        success: true,
        discordName: validation.discordName,
        userId: validation.discordId,
        sessionId: sessionId
    });
    
    console.log(`[Auth] User authenticated: ${validation.discordName} (MC: ${minecraft_name})`);
}

function handleJoinRoom(sessionId, data) {
    const session = sessions.get(sessionId);
    if (!session.userId) {
        sendToSession(sessionId, { type: 'error', message: 'Not authenticated' });
        return;
    }
    
    const roomId = data.room_id || data.roomId;
    const room = rooms.get(roomId);
    
    if (!room) {
        sendToSession(sessionId, { type: 'join_room_response', success: false, error: 'Room not found' });
        return;
    }
    
    if (room.userIds.size >= room.maxUsers) {
        sendToSession(sessionId, { type: 'join_room_response', success: false, error: 'Room is full' });
        return;
    }
    
    // Leave current room if in one
    if (session.roomId) {
        handleLeaveRoom(sessionId, true); // silent leave
    }
    
    // Join new room
    room.userIds.add(session.userId);
    session.roomId = roomId;
    
    // Notify all users in room
    broadcastToRoom(roomId, {
        type: 'user_joined',
        userId: session.userId,
        discordName: session.discordName,
        displayName: session.displayName
    }, session.userId);
    
    // Send room info to joining user
    sendToSession(sessionId, {
        type: 'join_room_response',
        success: true,
        room: getRoomInfo(roomId),
        users: getRoomUsers(roomId)
    });
    
    console.log(`[Room] ${session.discordName} joined room ${room.name}`);
}

function handleCreateRoom(sessionId, data) {
    const session = sessions.get(sessionId);
    if (!session.userId) {
        sendToSession(sessionId, { type: 'error', message: 'Not authenticated' });
        return;
    }
    
    const { name, isPrivate, linkedPartyId, maxUsers } = data;
    const roomId = uuidv4();
    
    rooms.set(roomId, {
        name: name || 'New Room',
        ownerId: session.userId,
        isPrivate: isPrivate || data.private || false,
        linkedPartyId: linkedPartyId || null,
        maxUsers: maxUsers || CONFIG.MAX_ROOM_USERS,
        userIds: new Set()
    });
    
    console.log(`[Room] Created: ${name} (${roomId}) by ${session.discordName}`);
    
    sendToSession(sessionId, {
        type: 'create_room_response',
        success: true,
        roomId,
        room: getRoomInfo(roomId)
    });
    
    // Auto-join creator to room
    handleJoinRoom(sessionId, { roomId });
}

function handleLeaveRoom(sessionId, silent = false) {
    const session = sessions.get(sessionId);
    if (!session || !session.roomId) return;
    
    const room = rooms.get(session.roomId);
    const oldRoomId = session.roomId;
    
    if (room) {
        room.userIds.delete(session.userId);
        
        // Notify others
        broadcastToRoom(oldRoomId, {
            type: 'user_left',
            userId: session.userId
        });
        
        // Delete room if empty
        if (room.userIds.size === 0) {
            rooms.delete(oldRoomId);
            console.log(`[Room] Deleted empty room: ${room.name}`);
        }
    }
    
    session.roomId = null;
    
    if (!silent) {
        sendToSession(sessionId, { type: 'left_room' });
    }
}

function handleMuteState(sessionId, data) {
    const session = sessions.get(sessionId);
    if (!session || !session.roomId) return;
    
    const user = users.get(session.userId);
    if (user) {
        user.muted = data.muted;
        
        broadcastToRoom(session.roomId, {
            type: 'user_mute_state',
            userId: session.userId,
            muted: data.muted
        });
    }
}

function handleDeafenState(sessionId, data) {
    const session = sessions.get(sessionId);
    if (!session || !session.roomId) return;
    
    const user = users.get(session.userId);
    if (user) {
        user.deafened = data.deafened;
        
        broadcastToRoom(session.roomId, {
            type: 'user_deafen_state',
            userId: session.userId,
            deafened: data.deafened
        });
    }
}

function handleListRooms(sessionId) {
    const roomList = [];
    
    for (const [roomId, room] of rooms) {
        if (!room.isPrivate) {
            roomList.push({
                id: roomId,
                name: room.name,
                userCount: room.userIds.size,
                maxUsers: room.maxUsers,
                linkedPartyId: room.linkedPartyId
            });
        }
    }
    
    sendToSession(sessionId, { type: 'room_list', rooms: roomList });
}

function handleDisconnect(sessionId) {
    const session = sessions.get(sessionId);
    if (session) {
        handleLeaveRoom(sessionId, true);
        
        if (session.userId) {
            users.delete(session.userId);
        }
        
        try {
            session.socket.destroy();
        } catch (e) {}
    }
    
    sessions.delete(sessionId);
}

// UDP Server (Audio)
const udpServer = dgram.createSocket('udp4');

udpServer.on('message', (data, rinfo) => {
    // Packet format: [type(1)] [sessionId(36)] [audio_data(n)]
    if (data.length < 37) return;
    
    const type = data[0];
    const sessionId = data.slice(1, 37).toString();
    const audioData = data.slice(37);
    
    const session = sessions.get(sessionId);
    if (!session) return;
    
    // Store UDP address for this session
    if (!session.udpAddress) {
        session.udpAddress = rinfo.address;
        session.udpPort = rinfo.port;
        console.log(`[UDP] Registered ${sessionId} at ${rinfo.address}:${rinfo.port}`);
    }
    
    // Forward audio to all other users in the same room
    if (session.roomId && type === 0x01) { // 0x01 = audio packet
        const room = rooms.get(session.roomId);
        if (!room) return;
        
        // Check if user is muted
        const user = users.get(session.userId);
        if (user && user.muted) return;
        
        for (const odId of room.userIds) {
            if (odId === session.userId) continue; // Don't send to self
            
            const targetUser = users.get(odId);
            if (!targetUser) continue;
            
            // Check if target is deafened
            if (targetUser.deafened) continue;
            
            const targetSession = sessions.get(targetUser.sessionId);
            if (!targetSession || !targetSession.udpAddress) continue;
            
            // Build outgoing packet: [type(1)] [senderId(36)] [audio_data(n)]
            const outPacket = Buffer.concat([
                Buffer.from([0x01]),
                Buffer.from(session.userId),
                audioData
            ]);
            
            udpServer.send(outPacket, targetSession.udpPort, targetSession.udpAddress);
        }
    }
});

udpServer.on('error', (err) => {
    console.error('[UDP] Error:', err);
});

udpServer.bind(CONFIG.UDP_PORT, () => {
    console.log(`[UDP] Listening on port ${CONFIG.UDP_PORT}`);
});

// Helper functions
function sendToSession(sessionId, message) {
    const session = sessions.get(sessionId);
    if (session && session.socket && !session.socket.destroyed) {
        try {
            session.socket.write(JSON.stringify(message) + '\n');
        } catch (err) {
            console.error(`[TCP] Error sending to ${sessionId}:`, err.message);
        }
    }
}

function broadcastToRoom(roomId, message, excludeUserId = null) {
    const room = rooms.get(roomId);
    if (!room) return;
    
    for (const userId of room.userIds) {
        if (userId === excludeUserId) continue;
        
        const user = users.get(userId);
        if (!user) continue;
        
        sendToSession(user.sessionId, message);
    }
}

function getRoomInfo(roomId) {
    const room = rooms.get(roomId);
    if (!room) return null;
    
    return {
        id: roomId,
        name: room.name,
        ownerId: room.ownerId,
        isPrivate: room.isPrivate,
        linkedPartyId: room.linkedPartyId,
        maxUsers: room.maxUsers,
        userCount: room.userIds.size
    };
}

function getRoomUsers(roomId) {
    const room = rooms.get(roomId);
    if (!room) return [];
    
    const userList = [];
    for (const odId of room.userIds) {
        const user = users.get(odId);
        if (user) {
            userList.push({
                odId: odId,
                discordName: user.discordName,
                displayName: user.displayName,
                muted: user.muted,
                deafened: user.deafened
            });
        }
    }
    
    return userList;
}

// Heartbeat check - disconnect inactive sessions
setInterval(() => {
    const now = Date.now();
    for (const [sessionId, session] of sessions) {
        if (now - session.lastHeartbeat > CONFIG.SESSION_TIMEOUT) {
            console.log(`[Heartbeat] Session timeout: ${sessionId}`);
            handleDisconnect(sessionId);
        }
    }
}, CONFIG.HEARTBEAT_INTERVAL);

// Startup
console.log('=================================');
console.log('  RavenClient Voice Server');
console.log('=================================');
console.log(`TCP control on port ${CONFIG.TCP_PORT}`);
console.log(`UDP audio on port ${CONFIG.UDP_PORT}`);
console.log('');
console.log('Ready for connections...');
