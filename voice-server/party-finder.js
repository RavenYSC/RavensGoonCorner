/**
 * RavenClient Party Finder Server
 * 
 * REST API for party finder functionality
 * Run alongside the voice server on VPS
 * 
 * Run with:
 *   node party-finder.js
 * 
 * Port: 25580
 */

const http = require('http');
const crypto = require('crypto');

// UUID generator
function uuidv4() {
    return crypto.randomUUID();
}

// Configuration
const PORT = 25580;
const PARTY_TIMEOUT = 30 * 60 * 1000; // 30 minutes

// State
const parties = new Map();  // partyId -> party object
const playerParties = new Map(); // playerUUID -> partyId (for quick lookup)

// Clean up expired parties every 5 minutes
setInterval(() => {
    const now = Date.now();
    for (const [partyId, party] of parties) {
        if (now - party.createdAt > PARTY_TIMEOUT) {
            console.log(`[PartyFinder] Removing expired party: ${party.note || partyId}`);
            // Remove player mappings
            for (const member of party.members) {
                if (playerParties.get(member.uuid) === partyId) {
                    playerParties.delete(member.uuid);
                }
            }
            parties.delete(partyId);
        }
    }
}, 5 * 60 * 1000);

// Parse JSON body from request
function parseBody(req) {
    return new Promise((resolve, reject) => {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                resolve(body ? JSON.parse(body) : {});
            } catch (e) {
                reject(e);
            }
        });
        req.on('error', reject);
    });
}

// Parse query parameters
function parseQuery(url) {
    const params = {};
    const queryIndex = url.indexOf('?');
    if (queryIndex !== -1) {
        const queryString = url.substring(queryIndex + 1);
        for (const param of queryString.split('&')) {
            const [key, value] = param.split('=');
            // Replace + with space before decoding (Java URLEncoder uses + for spaces)
            const decodedKey = decodeURIComponent((key || '').replace(/\+/g, ' '));
            const decodedValue = decodeURIComponent((value || '').replace(/\+/g, ' '));
            params[decodedKey] = decodedValue;
        }
    }
    return params;
}

// Send JSON response
function sendJson(res, statusCode, data) {
    res.writeHead(statusCode, {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type'
    });
    res.end(JSON.stringify(data));
}

// HTTP Server
const server = http.createServer(async (req, res) => {
    // Handle CORS preflight
    if (req.method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type'
        });
        res.end();
        return;
    }

    const url = req.url.split('?')[0];
    const query = parseQuery(req.url);

    try {
        // Create Party
        if (req.method === 'POST' && url === '/partyfinder/create') {
            const body = await parseBody(req);
            
            const {
                player_name,
                player_uuid,
                category,
                category_color,
                note,
                min_level,
                max_players,
                filters
            } = body;

            if (!player_uuid || !category) {
                sendJson(res, 400, { error: 'Missing required fields' });
                return;
            }

            // Check if player already has a party
            const existingPartyId = playerParties.get(player_uuid);
            if (existingPartyId) {
                // Remove from old party
                const oldParty = parties.get(existingPartyId);
                if (oldParty) {
                    oldParty.members = oldParty.members.filter(m => m.uuid !== player_uuid);
                    if (oldParty.members.length === 0) {
                        parties.delete(existingPartyId);
                    }
                }
            }

            const partyId = uuidv4();
            const party = {
                id: partyId,
                leader: {
                    name: player_name,
                    uuid: player_uuid
                },
                category,
                categoryColor: category_color || 0xFFFFFF,
                note: note || '',
                minLevel: min_level || 0,
                maxPlayers: max_players || 5,
                filters: filters || {},
                members: [{
                    name: player_name,
                    uuid: player_uuid,
                    isLeader: true,
                    joinedAt: Date.now()
                }],
                createdAt: Date.now()
            };

            parties.set(partyId, party);
            playerParties.set(player_uuid, partyId);

            console.log(`[PartyFinder] Party created: ${note || category} by ${player_name}`);

            sendJson(res, 201, {
                success: true,
                party_id: partyId,
                party
            });
        }

        // List Parties
        else if (req.method === 'GET' && url === '/partyfinder/list') {
            const category = query.category || 'all';
            const minLevel = parseInt(query.min_level) || 0;

            console.log(`[PartyFinder] Listing parties for category: "${category}"`);
            console.log(`[PartyFinder] Total parties in memory: ${parties.size}`);

            const partyList = [];
            for (const [partyId, party] of parties) {
                console.log(`[PartyFinder] Checking party: "${party.category}" against "${category}"`);
                
                // Filter by category if specified (case-insensitive, partial match)
                if (category !== 'all') {
                    const searchCat = category.toLowerCase().trim();
                    const partyCat = party.category.toLowerCase().trim();
                    
                    // Match if party category starts with search, or search starts with party, or they're equal
                    if (!partyCat.startsWith(searchCat) && !searchCat.startsWith(partyCat) && partyCat !== searchCat) {
                        console.log(`[PartyFinder] Category mismatch, skipping`);
                        continue;
                    }
                }

                // Don't show full parties
                if (party.members.length >= party.maxPlayers) {
                    console.log(`[PartyFinder] Party full, skipping`);
                    continue;
                }

                console.log(`[PartyFinder] Adding party to list`);
                partyList.push({
                    id: party.id,
                    leader: party.leader.name,
                    category: party.category,
                    categoryColor: party.categoryColor,
                    note: party.note,
                    minLevel: party.minLevel,
                    filters: party.filters,
                    memberCount: party.members.length,
                    maxPlayers: party.maxPlayers,
                    createdAt: party.createdAt
                });
            }

            // Sort by creation time (newest first)
            partyList.sort((a, b) => b.createdAt - a.createdAt);

            sendJson(res, 200, {
                success: true,
                parties: partyList,
                total: partyList.length
            });
        }

        // Get Party Details
        else if (req.method === 'GET' && url.startsWith('/partyfinder/party/')) {
            const partyId = url.split('/').pop();
            const party = parties.get(partyId);

            if (!party) {
                sendJson(res, 404, { error: 'Party not found' });
                return;
            }

            sendJson(res, 200, {
                success: true,
                party: {
                    id: party.id,
                    leader: party.leader,
                    category: party.category,
                    categoryColor: party.categoryColor,
                    note: party.note,
                    minLevel: party.minLevel,
                    filters: party.filters,
                    members: party.members,
                    maxPlayers: party.maxPlayers,
                    createdAt: party.createdAt
                }
            });
        }

        // Join Party
        else if (req.method === 'POST' && url === '/partyfinder/join') {
            const body = await parseBody(req);
            const { party_id, player_name, player_uuid } = body;

            if (!party_id || !player_uuid) {
                sendJson(res, 400, { error: 'Missing required fields' });
                return;
            }

            const party = parties.get(party_id);
            if (!party) {
                sendJson(res, 404, { error: 'Party not found' });
                return;
            }

            if (party.members.length >= party.maxPlayers) {
                sendJson(res, 400, { error: 'Party is full' });
                return;
            }

            // Check if already in party
            if (party.members.some(m => m.uuid === player_uuid)) {
                sendJson(res, 400, { error: 'Already in this party' });
                return;
            }

            // Leave current party if in one
            const existingPartyId = playerParties.get(player_uuid);
            if (existingPartyId && existingPartyId !== party_id) {
                const oldParty = parties.get(existingPartyId);
                if (oldParty) {
                    oldParty.members = oldParty.members.filter(m => m.uuid !== player_uuid);
                    if (oldParty.members.length === 0) {
                        parties.delete(existingPartyId);
                    }
                }
            }

            // Add to party
            party.members.push({
                name: player_name,
                uuid: player_uuid,
                isLeader: false,
                joinedAt: Date.now()
            });
            playerParties.set(player_uuid, party_id);

            console.log(`[PartyFinder] ${player_name} joined party: ${party.note || party.category}`);

            sendJson(res, 200, {
                success: true,
                party_id: party_id,
                leader: party.leader.name,
                message: `Joined ${party.leader.name}'s party`
            });
        }

        // Leave Party
        else if (req.method === 'POST' && url === '/partyfinder/leave') {
            const body = await parseBody(req);
            const { player_uuid } = body;

            if (!player_uuid) {
                sendJson(res, 400, { error: 'Missing player_uuid' });
                return;
            }

            const partyId = playerParties.get(player_uuid);
            if (!partyId) {
                sendJson(res, 400, { error: 'Not in a party' });
                return;
            }

            const party = parties.get(partyId);
            if (!party) {
                playerParties.delete(player_uuid);
                sendJson(res, 200, { success: true });
                return;
            }

            // Remove from party
            party.members = party.members.filter(m => m.uuid !== player_uuid);
            playerParties.delete(player_uuid);

            // If leader left, assign new leader or delete party
            if (party.leader.uuid === player_uuid) {
                if (party.members.length > 0) {
                    party.members[0].isLeader = true;
                    party.leader = {
                        name: party.members[0].name,
                        uuid: party.members[0].uuid
                    };
                    console.log(`[PartyFinder] New leader: ${party.leader.name}`);
                } else {
                    parties.delete(partyId);
                    console.log(`[PartyFinder] Party disbanded: ${party.note || party.category}`);
                }
            }

            sendJson(res, 200, { success: true });
        }

        // Delete/Disband Party (leader only)
        else if (req.method === 'DELETE' && url.startsWith('/partyfinder/party/')) {
            const partyId = url.split('/').pop();
            const body = await parseBody(req);
            const { player_uuid } = body;

            const party = parties.get(partyId);
            if (!party) {
                sendJson(res, 404, { error: 'Party not found' });
                return;
            }

            if (party.leader.uuid !== player_uuid) {
                sendJson(res, 403, { error: 'Only the leader can disband the party' });
                return;
            }

            // Remove all player mappings
            for (const member of party.members) {
                playerParties.delete(member.uuid);
            }
            parties.delete(partyId);

            console.log(`[PartyFinder] Party disbanded by leader: ${party.note || party.category}`);

            sendJson(res, 200, { success: true });
        }

        // My Party
        else if (req.method === 'GET' && url === '/partyfinder/myparty') {
            const playerUuid = query.player_uuid;

            if (!playerUuid) {
                sendJson(res, 400, { error: 'Missing player_uuid' });
                return;
            }

            const partyId = playerParties.get(playerUuid);
            if (!partyId) {
                sendJson(res, 200, { success: true, party: null });
                return;
            }

            const party = parties.get(partyId);
            if (!party) {
                playerParties.delete(playerUuid);
                sendJson(res, 200, { success: true, party: null });
                return;
            }

            sendJson(res, 200, {
                success: true,
                party: {
                    id: party.id,
                    leader: party.leader,
                    category: party.category,
                    categoryColor: party.categoryColor,
                    note: party.note,
                    minLevel: party.minLevel,
                    filters: party.filters,
                    members: party.members,
                    maxPlayers: party.maxPlayers,
                    createdAt: party.createdAt,
                    isLeader: party.leader.uuid === playerUuid
                }
            });
        }

        // Health check
        else if (req.method === 'GET' && url === '/health') {
            sendJson(res, 200, {
                status: 'ok',
                parties: parties.size,
                uptime: process.uptime()
            });
        }

        // 404
        else {
            sendJson(res, 404, { error: 'Not found' });
        }

    } catch (error) {
        console.error('[PartyFinder] Error:', error);
        sendJson(res, 500, { error: 'Internal server error' });
    }
});

server.listen(PORT, () => {
    console.log('=================================');
    console.log('  RavenClient Party Finder API');
    console.log('=================================');
    console.log(`Listening on port ${PORT}`);
    console.log('');
    console.log('Endpoints:');
    console.log('  POST /partyfinder/create');
    console.log('  GET  /partyfinder/list');
    console.log('  GET  /partyfinder/party/:id');
    console.log('  POST /partyfinder/join');
    console.log('  POST /partyfinder/leave');
    console.log('  DELETE /partyfinder/party/:id');
    console.log('  GET  /partyfinder/myparty');
    console.log('  GET  /health');
    console.log('');
    console.log('Ready for connections...');
});
