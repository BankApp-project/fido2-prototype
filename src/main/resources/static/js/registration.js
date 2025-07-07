const registrationButton = document.getElementById("registration-button");
registrationButton.addEventListener('click', async () => {
    console.log("Registration button was clicked")

    try {
        /** @type {HTMLInputElement} */
        const usernameElement = document.getElementById("username-input");
        const username = usernameElement.value;

        const response = await fetch('api/auth/registration/start', {
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

        const responseBodyText = await response.text();
        //check response body
        console.log("response body: " + responseBodyText)

        const publicKeyCredentialCreationOptionsJSON = JSON.parse(responseBodyText);
        console.log("response body @JSON: " + publicKeyCredentialCreationOptionsJSON)

        const credentialCreationOptions =
            PublicKeyCredential.parseCreationOptionsFromJSON(publicKeyCredentialCreationOptionsJSON);
        console.log("parsed response body: " + credentialCreationOptions)
    } catch (error) {
        console.log("Registration failed:", error);
    }
})
