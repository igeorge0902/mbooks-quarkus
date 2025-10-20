package com.jeet.broadcasting.serialization;

import com.jeet.broadcasting.eventModel.AddMovie;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class Serialization {

    private static byte[] message;

    public String serializeAddMovie(AddMovie event) {

        // Send serialized object as encoded string 
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;

        try {

            out = new ObjectOutputStream(bos);
            out.writeObject(event);
            out.flush();
            out.close();

            message = bos.toByteArray();

            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }

        } catch (IOException e) {
            e.getMessage();
        }
        return new String(Base64.encodeBase64(message));
    }
}
