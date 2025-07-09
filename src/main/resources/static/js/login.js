const loginButton = document.getElementById("login-button");
loginButton.addEventListener('click', async () => {
    console.log("Login button was clicked")

    try {
        /** @type {HTMLInputElement} */
        const usernameElement = document.getElementById("login-username-input");
        const username = usernameElement.value;

        //its not needed to send username now, but it will be needed later on
        const response = await fetch('api/auth/challenge', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                'username': username
            })
        });
        // Check if the request was successful
        if (!response.ok) {
            throw new Error(`Server responded with ${response.status}`);
        }

        const sessionId = response.headers.get('Session-ID');
        const publicKeyCredentialCreationOptionsJSON = await response.json()
        console.log("response body @JSON: ", publicKeyCredentialCreationOptionsJSON)

        const credentialCreationOptions =
            PublicKeyCredential.parseCreationOptionsFromJSON(publicKeyCredentialCreationOptionsJSON);
        console.log("parsed response body: ", credentialCreationOptions)

        const publicKeyCredential = await navigator.credentials.get( {
            publicKey: credentialCreationOptions
        });

        const authenticationResponseJSON = publicKeyCredential.toJSON();
        await fetch("api/auth/auth", {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Session-ID': sessionId
            },
            body: JSON.stringify({
                'username': username,
                'authenticationResponseJSON': JSON.stringify(authenticationResponseJSON)
            })
        });

    } catch (error) {
        console.log("Authentication failed:", error);
    }
})
