package online.bankapp.fido2.prototype;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Fido2PrototypeApp {


    public static void main(String[] args) {
        SpringApplication.run(Fido2PrototypeApp.class, args);
        log.info("App started!");
    }
}
