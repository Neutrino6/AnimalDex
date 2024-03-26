package it.uniroma1.animaldex;

import lombok.Data;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;

@RestController
public class CertificatesController {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @RequestMapping("/certificates")
    String certificates() {
        return 
            "<form action=\"http://localhost:5000/predict\" method=\"post\" enctype=\"multipart/form-data\">\r\n" + //
            "    <label for=\"fileInput\">Upload a certificate and get points:</label><br>\r\n" + //
            "    <input type=\"file\" id=\"fileInput\" name=\"fileInput\" accept=\"image/*\"><br><br>\r\n" + //
            "    <input type=\"submit\" value=\"Upload certificate\">\r\n" + //
            "</form>\r\n";
    }

    /*@RequestMapping("/upload")
    public String handleFileUpload(@RequestParam("fileInput") MultipartFile file) {
        try {
            // Salva l'immagine temporaneamente
            File tempFile = File.createTempFile("temp", null);
            file.transferTo(tempFile);

            // Costruisci il processo per eseguire lo script Python
            ProcessBuilder pb = new ProcessBuilder("python", "recognition.py", tempFile.getAbsolutePath());
            pb.directory(new File("src\\main\\resources\\recognition.py"));
            
            // Avvia il processo
            Process process = pb.start();
            
            // Attendere il completamento del processo
            int exitCode = process.waitFor();
            System.out.println("\nIl processo di analisi dell'immagine è terminato con il codice di uscita " + exitCode);

            // Rimuovi il file temporaneo
            tempFile.delete();

            return "Analisi dell'immagine completata!";
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Errore durante l'analisi dell'immagine.";
        }
    }*/

    /*@Data
    static class Result {
        private final int left;
        private final int right;
        private final long answer;
    }

    // SQL sample
    @RequestMapping("calc")
    Result calc(@RequestParam int left, @RequestParam int right) {
        MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("left", left)
                .addValue("right", right);
        return jdbcTemplate.queryForObject("SELECT :left + :right AS answer", source,
                (rs, rowNum) -> new Result(left, right, rs.getLong("answer")));
    }*/
}
