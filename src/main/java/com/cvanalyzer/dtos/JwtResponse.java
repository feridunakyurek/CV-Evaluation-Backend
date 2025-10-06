package com.cvanalyzer.dtos;
import lombok.Getter;
import lombok.Setter;

/**
 * JwtResponse, JWT (JSON Web Token) yanıtını temsil eden bir sınıftır.
 * Bu sınıf, kimlik doğrulama işlemi sonucunda kullanıcıya döndürülen JWT'yi saklar.
 */
@Setter // Tüm alanlar için setter metodları oluşturur.
@Getter // Tüm alanlar için getter metodları oluşturur.
public class JwtResponse {

    private String token; // JWT'yi temsil eden alan. Kullanıcı kimlik doğrulaması tamamlandıktan sonra döndürülen token.

    /**
     * JwtResponse sınıfının yapıcı metodudur.
     * Token değerini ayarlamak için kullanılır.
     * @param token JWT'yi temsil eden string değeri.
     */
    public JwtResponse(String token) {
        this.token = token; // Parametre olarak alınan token değerini sınıfın alanına atar.
    }
}
