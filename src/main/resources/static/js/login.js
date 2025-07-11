const loginButton = document.getElementById("login-button");
loginButton.addEventListener('click', async () => {
    console.log("Login button was clicked")

    try {
        /** @type {HTMLInputElement} */
        const usernameElement = document.getElementById("login-username-input");
        const username = usernameElement.value;

        //its not needed to send username now, but it will be needed later on
        const response = await fetch('api/auth/login/challenge/', {
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

        const publicKeyCredentialRequestOptionsJSON = await response.json()
        console.log("response body @JSON: ", publicKeyCredentialRequestOptionsJSON)

        const credentialRequestOptions =
            PublicKeyCredential.parseRequestOptionsFromJSON(publicKeyCredentialRequestOptionsJSON);
        console.log("parsed response body: ", credentialRequestOptions)

        const publicKeyCredential = await navigator.credentials.get( {
            publicKey: credentialRequestOptions
        });

        const authenticationResponseJSON = publicKeyCredential.toJSON();
        const msg = await fetch("api/auth/login", {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Session-ID': response.headers.get('Session-ID')
            },
            body: JSON.stringify({
                'username': username,
                'authenticationResponseJSON': JSON.stringify(authenticationResponseJSON)
            })
        });

        const msgtext = await msg.text();
        alert(msgtext);

    } catch (error) {
        console.log("Authentication failed:", error);
    }
})
