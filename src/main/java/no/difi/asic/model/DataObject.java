package no.difi.asic.model;

import no.difi.asic.code.MessageDigestAlgorithm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author erlend
 */
public class DataObject implements Serializable {

    private static final long serialVersionUID = -1971339559017072869L;

    private Type type;

    private String filename;

    private MimeType mimeType;

    private Hash hash = new Hash();

    private List<Signer> signers = new ArrayList<>();

    public DataObject(String filename) {
        this.filename = filename;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    public Hash getHash() {
        return hash;
    }

    public void addSigner(Signer signer) {
        signers.add(signer);
    }

    boolean verify(Signer signer, MessageDigestAlgorithm algorithm, byte[] digest) {
        if (hash.verify(algorithm, digest)) {
            signers.add(signer);
            return true;
        }

        return false;
    }

    public List<Signer> getSigners() {
        return Collections.unmodifiableList(signers);
    }

    public enum Type {
        DATA, METADATA, MANIFEST, DETACHED_SIGNATURE
    }
}