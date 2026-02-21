const https = require('https');
const querystring = require('querystring');

const clientId = "1000.56NL5XKO8B4W1XBL3TEJ52F62LQIEM";
const clientSecret = "a197f6020c7998385e4500d9c9bab92135e63a3060";
const refreshToken = "1000.24c39422690ccfae19a20c0db16496a7.27ef70eb85d76ac49e5e691b32a42f33";
const projectId = "26566000000013009";

function request(options, postData) {
    return new Promise((resolve, reject) => {
        const req = https.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                try {
                    const parsed = JSON.parse(data);
                    resolve({ statusCode: res.statusCode, data: parsed });
                } catch (e) {
                    resolve({ statusCode: res.statusCode, data: data });
                }
            });
        });

        req.on('error', (e) => reject(e));

        if (postData) {
            req.write(postData);
        }
        req.end();
    });
}

async function main() {
    try {
        console.log("1. Refreshing Access Token...");
        const tokenPostData = querystring.stringify({
            refresh_token: refreshToken,
            client_id: clientId,
            client_secret: clientSecret,
            grant_type: 'refresh_token'
        });

        const tokenOptions = {
            hostname: 'accounts.zoho.in',
            path: '/oauth/v2/token',
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Content-Length': tokenPostData.length
            }
        };

        const tokenResp = await request(tokenOptions, tokenPostData);
        if (tokenResp.data.error) {
            throw new Error(`Token Error: ${JSON.stringify(tokenResp.data)}`);
        }
        const accessToken = tokenResp.data.access_token;
        console.log("   Access Token Acquired.");

        console.log("\n2. Fetching Project Environments...");
        // Try getting project details which might contain environments
        // or specific environment endpoint
        const envOptions = {
            hostname: 'api.catalyst.zoho.in',
            path: `/baas/v1/project/${projectId}/project_env_details`, // This endpoint often lists envs
            method: 'GET',
            headers: {
                'Authorization': `Zoho-oauthtoken ${accessToken}`
            }
        };

        // If the specific env path fails, we can try just /project/{id}
        let envResp = await request(envOptions);

        if (envResp.statusCode !== 200) {
            console.log(`   Direct env endpoint failed (${envResp.statusCode}), trying generic project endpoint...`);
            envOptions.path = `/baas/v1/project/${projectId}`;
            envResp = await request(envOptions);
        }

        console.log("\n--- API RESPONSE ---");
        console.log(JSON.stringify(envResp.data, null, 2));
        console.log("--------------------\n");

        if (envResp.data && envResp.data.data) {
            // Catalyst API response structure can vary, looking for environment lists
            const data = envResp.data.data;
            // Check if it's project details with environments
            // Or just list of envs

            // Inspecting structure manually based on output
        }

    } catch (error) {
        console.error("Error:", error.message);
    }
}

main();
