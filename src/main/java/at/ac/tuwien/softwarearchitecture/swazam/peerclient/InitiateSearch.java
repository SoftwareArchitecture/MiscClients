package at.ac.tuwien.softwarearchitecture.swazam.peerclient;

import ac.at.tuwien.infosys.swa.audio.Fingerprint;
import ac.at.tuwien.infosys.swa.audio.FingerprintSystem;
import ac.at.tuwien.infosys.swa.audio.util.*;
import at.ac.tuwien.softwarearchitecture.swazam.common.infos.Account;
import at.ac.tuwien.softwarearchitecture.swazam.common.infos.ClientInfo;
import at.ac.tuwien.softwarearchitecture.swazam.common.infos.FingerprintSearchRequest;
import java.io.BufferedReader;

import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.log4j.Level;

public class InitiateSearch {

    public static void main(String[] args) throws Exception {

        Fingerprint fingerprint = InitiateSearch.extractFingerprint("./Test.mp3");
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setClientID(1);
        clientInfo.setUsername("test");
        clientInfo.setPassword("test");
        clientInfo.setSessionKey("1");

        FingerprintSearchRequest f = new FingerprintSearchRequest(clientInfo, fingerprint);


        JAXBContext context = JAXBContext.newInstance(FingerprintSearchRequest.class);

        StringWriter stringWriter = new StringWriter();
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(f, stringWriter);


        try {

            URL url = url = new URL("http://localhost:8181/Server/webapi/searchmanagement/search");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/xml");

            String input = stringWriter.getBuffer().toString();

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();


            System.out.println(conn.getResponseMessage());


            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }

            conn.disconnect();

        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public static Fingerprint extractFingerprint(String musicFilePath) throws UnsupportedAudioFileException, IOException {

        AudioInputStream stream = AudioSystem.getAudioInputStream(new File(musicFilePath));

        final AudioInputStream input = new Converter(stream).toPCM().toMono().to8Bit().getAudioInputStream();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int read = input.read(buffer);
        while (-1 < read) {
            out.write(buffer, 0, read);
            read = input.read(buffer);
        }

        return Downsampler.FROM > input.getFormat().getSampleRate()
                ? new FingerprintSystem(input.getFormat().getSampleRate()).fingerprint(out.toByteArray())
                : new FingerprintSystem(Downsampler.TO).fingerprint(new Downsampler().downsample(out.toByteArray()));
    }

    public Account login(ClientInfo client) {
        URL url = null;
        HttpURLConnection connection = null;
        Account superPeerInfo = null;
        try {
            url = new URL("http://localhost:8181/Server/webapi/accountmanagement/login");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/xml");
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            JAXBContext jaxbContext = JAXBContext.newInstance(ClientInfo.class);
            jaxbContext.createMarshaller().marshal(client, os);
            os.flush();
            os.close();

            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            InputStream inputStream = connection.getInputStream();
            if (inputStream != null) {
                superPeerInfo = (Account) JAXBContext.newInstance(Account.class).createUnmarshaller().unmarshal(inputStream);
            }
            if (connection != null) {
                connection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return superPeerInfo;
        }
    }
}
