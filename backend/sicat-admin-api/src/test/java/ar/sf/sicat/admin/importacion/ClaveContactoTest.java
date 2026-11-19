package ar.sf.sicat.admin.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClaveContactoTest {

    @Test
    void mismoContactoConOtroFormato() {
        assertThat(ClaveContacto.de("Teléfono", "342 456-7890")).isEqualTo(ClaveContacto.de("telefono", "(342) 4567890"));
        assertThat(ClaveContacto.de("WhatsApp", "+54 9 342 1234567")).isEqualTo(ClaveContacto.de("whatsapp", "5493421234567"));
        assertThat(ClaveContacto.de("Instagram", "@museo")).isEqualTo(ClaveContacto.de("Instagram", "https://www.instagram.com/museo/"));
        assertThat(ClaveContacto.de("Web", "https://museo.gob.ar/")).isEqualTo(ClaveContacto.de("Web", "MUSEO.gob.ar"));
        assertThat(ClaveContacto.de("Facebook", "facebook.com/MuseoSF")).isEqualTo(ClaveContacto.de("Facebook", "https://m.facebook.com/museosf"));
    }

    @Test
    void distintoTipoOValorNoEsElMismo() {
        assertThat(ClaveContacto.de("Teléfono", "3424567890")).isNotEqualTo(ClaveContacto.de("WhatsApp", "3424567890"));
        assertThat(ClaveContacto.de("Web", "museo.gob.ar")).isNotEqualTo(ClaveContacto.de("Web", "museo.gob.ar/visitas"));
        assertThat(ClaveContacto.de("Correo", "info@museo.ar")).isNotEqualTo(ClaveContacto.de("Correo", "museo@museo.ar"));
    }
}
