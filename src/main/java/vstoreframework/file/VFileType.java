package vstoreframework.file;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains constants that denote the supported mime types of the vStore framework.
 */
public final class VFileType {

    private VFileType() {}

    // Supported image types
    public static final String IMAGE_JPG = "image/jpeg";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_GIF = "image/gif";
    public static final String IMAGE_BMP = "image/bmp";

    public static final List<String> IMAGE_TYPES
            = Arrays.asList(IMAGE_JPG, IMAGE_PNG, IMAGE_GIF, IMAGE_BMP);

    //Supported video types
    public static final String VIDEO_MP4 = "video/mp4";
    public static final String VIDEO_MOV = "video/mov";
    public static final String VIDEO_3GP = "video/3gpp";

    public static final List<String> VIDEO_TYPES
            = Arrays.asList(VIDEO_MP4, VIDEO_MOV, VIDEO_3GP);

    //Supported document types
    public static final String DOC_DOC = "application/msword";
    public static final String DOC_XLS = "application/msexcel";
    public static final String DOC_PDF = "application/pdf";
    public static final String DOC_TXT = "application/txt";

    public static final List<String> DOC_TYPES
            = Arrays.asList(DOC_DOC, DOC_XLS, DOC_PDF, DOC_TXT);

    //Supported contact types
    public static final String CONTACT_VCF = "text/vcard";

    public static final List<String> CONTACT_TYPES
            = Arrays.asList(CONTACT_VCF);

    //Supported audio types
    public static final String AUDIO_MP3 = "audio/mpeg";
    public static final String AUDIO_M4A = "audio/mp4";
    public static final String AUDIO_WAV = "audio/wav";
    public static final String AUDIO_AAC = "audio/aac";

    public static final List<String> AUDIO_TYPES
            = Arrays.asList(AUDIO_MP3, AUDIO_M4A, AUDIO_WAV, AUDIO_AAC);

    //Unknown types
    public static final String MIME_UNKNOWN = "vmime/unknown";

    //Convenient map for requesting mime type based on file extension
    private static final HashMap<String, String> extToMimeMap;
    static
    {
        extToMimeMap = new HashMap<String, String>();
        extToMimeMap.put("jpg", IMAGE_JPG);
        extToMimeMap.put("jpeg", IMAGE_JPG);
        extToMimeMap.put("png", IMAGE_PNG);
        extToMimeMap.put("gif", IMAGE_GIF);
        extToMimeMap.put("bmp", IMAGE_BMP);

        extToMimeMap.put("mp4", VIDEO_MP4);
        extToMimeMap.put("mov", VIDEO_MOV);
        extToMimeMap.put("3gp", VIDEO_3GP);

        extToMimeMap.put("doc", DOC_DOC);
        extToMimeMap.put("xls", DOC_XLS);
        extToMimeMap.put("pdf", DOC_PDF);
        extToMimeMap.put("txt", DOC_TXT);

        extToMimeMap.put("vcf", CONTACT_VCF);

        extToMimeMap.put("mp3", AUDIO_MP3);
        extToMimeMap.put("m4a", AUDIO_M4A);
        extToMimeMap.put("wav", AUDIO_WAV);
        extToMimeMap.put("aac", AUDIO_AAC);

        extToMimeMap.put("vstor", MIME_UNKNOWN);
    }

    public static String getMimeTypeFromExtension(String extension) {
        return extToMimeMap.get(extension);
    }

    /**
     * This method iterates over the map of possible mime types and returns the first extension
     * for which the mime type matches.
     * @param mimetype The mime type to get the extension for.
     * @return The file extension corresponding to the given mime type.
     * Will be the default extension "vstor" if the mime type is not supported.
     */
    public static String getExtensionFromMimeType(String mimetype) {
        for(Map.Entry<String, String> set : extToMimeMap.entrySet()) {
            if(set.getValue().equals(mimetype)) {
                return set.getKey();
            }
        }
        return "vstor";
    }

    /**
     * Get a list of file types that are supported by the vStore framework.
     * @return Returns a HashMap where the keys represent file extensions and the values
     *         represent the corresponding MIME type.
     */
    public static HashMap<String, String> getSupportedTypes() {
        return extToMimeMap;
    }

    /**
     * This method checks, if the given mime type is supported by the framework.
     *
     * @param mimetype The mimetype to check.
     * @return True, if the file type is supported. False, if not.
     */
    public static boolean isMimeTypeSupported(String mimetype) {
        if(extToMimeMap.containsValue(mimetype)) {
            return true;
        }
        return false;
    }
    /**
     * This method checks, if the given file extension is supported by the framework.
     *
     * @param mimetype The file extension to check.
     * @return True, if the file extension is supported. False, if not.
     */
    public static boolean isExtensionSupported(String mimetype) {
        if(extToMimeMap.containsKey(mimetype)) {
            return true;
        }
        return false;
    }

}
