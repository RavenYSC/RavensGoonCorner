/**
 * RavenClient Voice Chat - Discord Authentication Bot
 * 
 * This bot generates authentication tokens for users to connect to the voice server.
 * Users link their Discord account, receive a token, and use it in the Minecraft client.
 * 
 * Dependencies:
 *   npm install discord.js crypto
 * 
 * Run with:
 *   node discord-bot.js
 * 
 * Environment Variables (create .env file or set directly):
 *   DISCORD_BOT_TOKEN=your_bot_token_here
 *   VOICE_SERVER_SECRET=a_random_secret_key_for_token_signing
 */

const { Client, GatewayIntentBits, EmbedBuilder, SlashCommandBuilder, REST, Routes, PermissionFlagsBits } = require('discord.js');
const crypto = require('crypto');
const axios = require('axios');

// Configuration
const CONFIG = {
    BOT_TOKEN: process.env.DISCORD_BOT_TOKEN || 'MTQ1Mzk3NjI4NTU1MzM2MTEzMw.Gwva0W.v_hBNjD0VpfKgwta_LJQw_3d_o9lLw8LQNYf0U',
    VOICE_SERVER_SECRET: process.env.VOICE_SERVER_SECRET || 'raven_voice_secret_change_me',
    TOKEN_EXPIRY_HOURS: 24 * 7, // Tokens valid for 7 days
    GUILD_ID: process.env.DISCORD_GUILD_ID || '1393103057151197255', // RavenClient Discord
    CLIENT_ID: process.env.DISCORD_CLIENT_ID || '1453976285553361133',
    LICENSE_API_URL: 'http://100.42.184.35:25582',
    LICENSE_ADMIN_KEY: 'RavenClient2025SecureAdminKey'
};

// Token storage (in production, use Redis or a database)
const activeTokens = new Map(); // token -> { odId, discordName, minecraftName, createdAt, expiresAt }
const userTokens = new Map();    // odId -> token

// Create Discord client
const client = new Client({
    intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages
    ]
});

// Generate secure token
function generateToken(discordId, discordName) {
    const timestamp = Date.now();
    const data = `${discordId}:${timestamp}:${CONFIG.VOICE_SERVER_SECRET}`;
    const hash = crypto.createHash('sha256').update(data).digest('hex');
    return hash.substring(0, 32); // 32 character token
}

// Create token for user
function createUserToken(discordId, discordName, minecraftName = null) {
    // Revoke old token if exists
    const oldToken = userTokens.get(discordId);
    if (oldToken) {
        activeTokens.delete(oldToken);
    }
    
    const token = generateToken(discordId, discordName);
    const now = Date.now();
    const expiresAt = now + (CONFIG.TOKEN_EXPIRY_HOURS * 60 * 60 * 1000);
    
    const tokenData = {
        discordId,
        discordName,
        minecraftName,
        createdAt: now,
        expiresAt
    };
    
    activeTokens.set(token, tokenData);
    userTokens.set(discordId, token);
    
    return { token, expiresAt };
}

// Validate token (called by voice server)
function validateToken(token) {
    const data = activeTokens.get(token);
    if (!data) {
        return { valid: false, error: 'Token not found' };
    }
    
    if (Date.now() > data.expiresAt) {
        activeTokens.delete(token);
        userTokens.delete(data.discordId);
        return { valid: false, error: 'Token expired' };
    }
    
    return {
        valid: true,
        discordId: data.discordId,
        discordName: data.discordName,
        minecraftName: data.minecraftName
    };
}

// Register slash commands
async function registerCommands() {
    const commands = [
        new SlashCommandBuilder()
            .setName('voicetoken')
            .setDescription('Generate a voice chat authentication token')
            .addStringOption(option =>
                option.setName('minecraft_name')
                    .setDescription('Your Minecraft username')
                    .setRequired(false)
            ),
        new SlashCommandBuilder()
            .setName('voicestatus')
            .setDescription('Check your current voice chat token status'),
        new SlashCommandBuilder()
            .setName('voicerevoke')
            .setDescription('Revoke your current voice chat token'),
        // License commands
        new SlashCommandBuilder()
            .setName('getkey')
            .setDescription('Get your RavenClient license key'),
        new SlashCommandBuilder()
            .setName('resetkey')
            .setDescription('Reset your license HWID to use on a different device'),
        new SlashCommandBuilder()
            .setName('revokekey')
            .setDescription('Revoke a user\'s license (Admin only)')
            .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
            .addUserOption(option =>
                option.setName('user')
                    .setDescription('The user whose license to revoke')
                    .setRequired(true)
            ),
        new SlashCommandBuilder()
            .setName('givekey')
            .setDescription('Generate a license key for another user (Admin only)')
            .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
            .addUserOption(option =>
                option.setName('user')
                    .setDescription('The user to give a license to')
                    .setRequired(true)
            )
            .addIntegerOption(option =>
                option.setName('days')
                    .setDescription('Number of days until expiration (leave empty for permanent)')
                    .setRequired(false)
            )
    ].map(cmd => cmd.toJSON());
    
    const rest = new REST({ version: '10' }).setToken(CONFIG.BOT_TOKEN);
    
    try {
        console.log('[Discord] Registering slash commands...');
        
        if (CONFIG.GUILD_ID) {
            await rest.put(
                Routes.applicationGuildCommands(CONFIG.CLIENT_ID, CONFIG.GUILD_ID),
                { body: commands }
            );
        } else {
            await rest.put(
                Routes.applicationCommands(CONFIG.CLIENT_ID),
                { body: commands }
            );
        }
        
        console.log('[Discord] Slash commands registered!');
    } catch (err) {
        console.error('[Discord] Error registering commands:', err);
    }
}

// Handle interactions
client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    
    const { commandName, user } = interaction;
    
    switch (commandName) {
        case 'voicetoken': {
            const minecraftName = interaction.options.getString('minecraft_name');
            const discordName = `${user.username}#${user.discriminator}`;
            
            const { token, expiresAt } = createUserToken(user.id, discordName, minecraftName);
            const expiresDate = new Date(expiresAt);
            
            const embed = new EmbedBuilder()
                .setColor(0x5865F2)
                .setTitle('🎤 Voice Chat Token Generated')
                .setDescription('Use this token in the Minecraft client to authenticate with voice chat.')
                .addFields(
                    { name: 'Your Token', value: `\`\`\`${token}\`\`\``, inline: false },
                    { name: 'Expires', value: expiresDate.toUTCString(), inline: true },
                    { name: 'Discord Account', value: discordName, inline: true }
                )
                .setFooter({ text: 'Copy this token and paste it in Voice Chat Settings in-game' })
                .setTimestamp();
            
            if (minecraftName) {
                embed.addFields({ name: 'Minecraft Name', value: minecraftName, inline: true });
            }
            
            // Send as ephemeral (only visible to user)
            await interaction.reply({ embeds: [embed], ephemeral: true });
            
            console.log(`[Token] Generated for ${discordName} (MC: ${minecraftName || 'not set'})`);
            break;
        }
        
        case 'voicestatus': {
            const token = userTokens.get(user.id);
            const discordName = `${user.username}#${user.discriminator}`;
            
            if (!token) {
                await interaction.reply({
                    content: '❌ You don\'t have an active voice chat token. Use `/voicetoken` to generate one.',
                    ephemeral: true
                });
                return;
            }
            
            const data = activeTokens.get(token);
            if (!data || Date.now() > data.expiresAt) {
                activeTokens.delete(token);
                userTokens.delete(user.id);
                await interaction.reply({
                    content: '❌ Your token has expired. Use `/voicetoken` to generate a new one.',
                    ephemeral: true
                });
                return;
            }
            
            const expiresDate = new Date(data.expiresAt);
            const timeLeft = Math.floor((data.expiresAt - Date.now()) / (1000 * 60 * 60));
            
            const embed = new EmbedBuilder()
                .setColor(0x55FF55)
                .setTitle('✅ Voice Chat Token Active')
                .addFields(
                    { name: 'Token', value: `\`\`\`${token}\`\`\``, inline: false },
                    { name: 'Expires', value: expiresDate.toUTCString(), inline: true },
                    { name: 'Time Remaining', value: `${timeLeft} hours`, inline: true }
                )
                .setTimestamp();
            
            await interaction.reply({ embeds: [embed], ephemeral: true });
            break;
        }
        
        case 'voicerevoke': {
            const token = userTokens.get(user.id);
            
            if (!token) {
                await interaction.reply({
                    content: '❌ You don\'t have an active token to revoke.',
                    ephemeral: true
                });
                return;
            }
            
            activeTokens.delete(token);
            userTokens.delete(user.id);
            
            await interaction.reply({
                content: '✅ Your voice chat token has been revoked. You will be disconnected from voice chat.',
                ephemeral: true
            });
            
            console.log(`[Token] Revoked for ${user.username}`);
            break;
        }
        
        // ============================================
        // LICENSE COMMANDS
        // ============================================
        
        case 'getkey': {
            try {
                const response = await axios.post(`${CONFIG.LICENSE_API_URL}/license/create`, {
                    username: user.username,
                    discordId: user.id,
                    adminKey: CONFIG.LICENSE_ADMIN_KEY
                });
                
                if (response.data.success) {
                    // Try to DM the key
                    try {
                        const embed = new EmbedBuilder()
                            .setColor(0x4a9eff)
                            .setTitle('🔑 Your RavenClient License Key')
                            .setDescription('Your license key has been generated!')
                            .addFields(
                                { name: 'License Key', value: `\`${response.data.key}\``, inline: false },
                                { name: 'Username', value: user.username, inline: true },
                                { name: 'Expires', value: response.data.expiresAt || 'Never', inline: true }
                            )
                            .setFooter({ text: 'Do not share this key with anyone!' })
                            .setTimestamp();
                        
                        await user.send({ embeds: [embed] });
                        await interaction.reply({ 
                            content: '✅ Your license key has been sent to your DMs!', 
                            ephemeral: true 
                        });
                    } catch (dmError) {
                        // DMs disabled, show in ephemeral
                        await interaction.reply({ 
                            content: `🔑 Your license key: \`${response.data.key}\`\n⚠️ Could not DM you - please enable DMs!`,
                            ephemeral: true 
                        });
                    }
                    console.log(`[License] Generated key for ${user.username}`);
                } else {
                    if (response.data.existingKey) {
                        await interaction.reply({ 
                            content: `❌ You already have an active license: \`${response.data.existingKey}\``, 
                            ephemeral: true 
                        });
                    } else {
                        await interaction.reply({ 
                            content: `❌ Error: ${response.data.error}`, 
                            ephemeral: true 
                        });
                    }
                }
            } catch (error) {
                console.error('[License] API error:', error.message);
                await interaction.reply({ 
                    content: '❌ Failed to generate license key. Please try again later.', 
                    ephemeral: true 
                });
            }
            break;
        }
        
        case 'resetkey': {
            try {
                const response = await axios.post(`${CONFIG.LICENSE_API_URL}/license/reset-hwid`, {
                    discordId: user.id,
                    adminKey: CONFIG.LICENSE_ADMIN_KEY
                });
                
                if (response.data.success) {
                    await interaction.reply({ 
                        content: '✅ Your license HWID has been reset! You can now use your key on a different device.', 
                        ephemeral: true 
                    });
                    console.log(`[License] Reset HWID for ${user.username}`);
                } else {
                    await interaction.reply({ 
                        content: `❌ Error: ${response.data.error || 'Unknown error'}`, 
                        ephemeral: true 
                    });
                }
            } catch (error) {
                if (error.response?.status === 404) {
                    await interaction.reply({ 
                        content: '❌ No license found for your account. Use `/getkey` to get one!', 
                        ephemeral: true 
                    });
                } else {
                    await interaction.reply({ 
                        content: '❌ Failed to reset HWID. Please try again later.', 
                        ephemeral: true 
                    });
                }
            }
            break;
        }
        
        case 'revokekey': {
            const targetUser = interaction.options.getUser('user');
            
            try {
                const response = await axios.post(`${CONFIG.LICENSE_API_URL}/license/revoke`, {
                    discordId: targetUser.id,
                    adminKey: CONFIG.LICENSE_ADMIN_KEY
                });
                
                if (response.data.success) {
                    await interaction.reply({ 
                        content: `✅ License for ${targetUser.username} has been revoked.`, 
                        ephemeral: true 
                    });
                    console.log(`[License] Revoked key for ${targetUser.username} by ${user.username}`);
                } else {
                    await interaction.reply({ 
                        content: `❌ Error: ${response.data.error}`, 
                        ephemeral: true 
                    });
                }
            } catch (error) {
                if (error.response?.status === 404) {
                    await interaction.reply({ 
                        content: `❌ No license found for ${targetUser.username}.`, 
                        ephemeral: true 
                    });
                } else {
                    await interaction.reply({ 
                        content: '❌ Failed to revoke license.', 
                        ephemeral: true 
                    });
                }
            }
            break;
        }
        
        case 'givekey': {
            const targetUser = interaction.options.getUser('user');
            const expiresIn = interaction.options.getInteger('days') || null;
            
            try {
                const response = await axios.post(`${CONFIG.LICENSE_API_URL}/license/create`, {
                    username: targetUser.username,
                    discordId: targetUser.id,
                    expiresIn: expiresIn,
                    adminKey: CONFIG.LICENSE_ADMIN_KEY
                });
                
                if (response.data.success) {
                    await interaction.reply({ 
                        content: `✅ License created for ${targetUser.username}: \`${response.data.key}\`${expiresIn ? ` (expires in ${expiresIn} days)` : ' (permanent)'}`, 
                        ephemeral: true 
                    });
                    console.log(`[License] Created key for ${targetUser.username} by ${user.username}`);
                } else {
                    await interaction.reply({ 
                        content: `❌ Error: ${response.data.error}`, 
                        ephemeral: true 
                    });
                }
            } catch (error) {
                await interaction.reply({ 
                    content: '❌ Failed to create license.', 
                    ephemeral: true 
                });
            }
            break;
        }
    }
});

// HTTP API for voice server to validate tokens
const http = require('http');

const apiServer = http.createServer((req, res) => {
    // CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', 'application/json');
    
    if (req.method === 'POST' && req.url === '/validate') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const { token } = JSON.parse(body);
                const result = validateToken(token);
                res.writeHead(200);
                res.end(JSON.stringify(result));
            } catch (err) {
                res.writeHead(400);
                res.end(JSON.stringify({ valid: false, error: 'Invalid request' }));
            }
        });
    } else if (req.method === 'GET' && req.url === '/health') {
        res.writeHead(200);
        res.end(JSON.stringify({ status: 'ok', activeTokens: activeTokens.size }));
    } else {
        res.writeHead(404);
        res.end(JSON.stringify({ error: 'Not found' }));
    }
});

const API_PORT = 25568;

// Bot ready
client.once('ready', async () => {
    console.log('=================================');
    console.log('  RavenClient Voice Auth Bot');
    console.log('=================================');
    console.log(`Logged in as ${client.user.tag}`);
    console.log('');
    
    await registerCommands();
    
    // Start API server
    apiServer.listen(API_PORT, () => {
        console.log(`[API] Token validation API running on port ${API_PORT}`);
    });
    
    console.log('');
    console.log('Bot is ready!');
});

// Login
client.login(CONFIG.BOT_TOKEN);
