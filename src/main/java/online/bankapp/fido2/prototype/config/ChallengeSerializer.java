package online.bankapp.fido2.prototype.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.webauthn4j.data.client.challenge.Challenge;

import java.io.IOException;
import java.util.Base64;

public class ChallengeSerializer extends JsonSerializer<Challenge> {

    @Override
    public void serialize(Challenge value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String base64UrlString = Base64.getUrlEncoder().withoutPadding().encodeToString(value.getValue());
        gen.writeString(base64UrlString);
    }
}
