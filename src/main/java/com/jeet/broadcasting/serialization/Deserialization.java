package com.jeet.broadcasting.serialization;

import com.jeet.broadcasting.eventModel.AddMovie;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

public class Deserialization {

    public AddMovie deserializeAddMovie(String message) {

        // Receive encoded string as message
        AddMovie event = new AddMovie();
        byte[] yourBytes2 = Base64.decodeBase64(message);

        ByteArrayInputStream bis = new ByteArrayInputStream(yourBytes2);
        ObjectInput in = null;

        try {
            // deserialize the byteArray
            in = new ObjectInputStream(bis);
            event = (AddMovie) in.readObject();

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();

        } finally {
            try {
                bis.close();
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.getMessage();
            }
        }
        return event;
    }
}
