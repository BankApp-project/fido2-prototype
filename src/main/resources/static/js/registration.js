const emailButton = document.getElementById("email-button");
emailButton.addEventListener('click', async () => {
    console.log("Email button was clicked")
    //there will be logic to send request with email to BE, but we wont implement it in this prototype
})

const pinButton = document.getElementById("pin-button");
pinButton.addEventListener('click', async () => {
    console.log("Pin confirmation button was clicked");

    try {
        /** @type {HTMLInputElement} */
        const emailElement = document.getElementById("email-input");
        const email = emailElement.value;

        const response = await fetch('api/auth/registration/challenge', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                'username': email
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

        const publicKeyCredential = await navigator.credentials.create( {
            publicKey: credentialCreationOptions
        });

        const registrationResponseJSON = publicKeyCredential.toJSON();
        const msg = await fetch("api/auth/registration/finish", {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Session-ID': sessionId
            },
            body: JSON.stringify({
                'username': email,
                'registrationResponseJSON': JSON.stringify(registrationResponseJSON)
            })
        });


        if (!msg.ok) {
            alert("Registration failed. Please try again.");
            showEmailView();
            throw new Error(`Server responded with ${msg.status}`);
        } else {
            const msgtext = await msg.text();
            console.log("msgtext: ", msgtext);
            alert(msgtext);
            showLoggedInView();
        }

    } catch (error) {
        console.log("Registration failed:", error);
    }
})