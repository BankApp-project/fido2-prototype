const continueButton = document.getElementById("continue-button");
continueButton.addEventListener('click', async () => {
    console.log("Continue button was clicked")

    try {

        const response = await fetch('api/auth/login/challenge/', {
            method: 'GET'
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
        const msg = await fetch('api/auth/login/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Session-ID': response.headers.get('Session-ID')
            },
            body: JSON.stringify({
                'authenticationResponseJSON': JSON.stringify(authenticationResponseJSON)
            })
        });

        //fallback
        console.log("msg status: ", msg.status);
        if (!msg.ok) {
            alert("Authentication failed. Please provide your email address.");
            showEmailView();
        } else {
            const msgtext = await msg.text();
            console.log("msgtext: ", msgtext);
            alert(msgtext);
        }

    } catch (error) {
        console.log("Authentication failed:", error);
    }
})
