package vstore.framework;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import py4j.GatewayServer;
import vstore.framework.exceptions.VStoreException;

/**
 * Class for running the framework as a runnable jar.
 * Starts a py4j gateway for communication from python.
 */
public class ServiceRunner {

    /**
     * Optional: Start vStore as a service
     * @param args First parameter: Working directory of vstore
     *             Second parameter: address of the master node
     */
    public static void main(String[] args) {

        if(args.length != 2) {
            System.out.println("Error: Please supply exactly two arguments: The storage path and the address of the master node!");
            return;
        }

        File storagePath;
        URL masterUri;

        //Read first argument (path to storage directory)
        String path = args[0];
        try {
            storagePath = new File(path);
            if (!storagePath.exists()) {
                System.out.println("Error: Invalid path supplied.");
                return;
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
            System.out.println("Error: Invalid path supplied.");
            return;
        }

        //Read second argument (uri to master node)
        String masterUriStr = args[1];
        try {
            masterUri = new URL(masterUriStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.out.println("Error: Invalid url supplied. Should be https://<ip-address>:<port>");
            return;
        }

        //Initialize VStore
        try {
            VStore.initialize(storagePath, masterUri);
        } catch(VStoreException e) {
            e.printStackTrace();
            System.err.println("Error while initializing the vStore framework.");
            return;
        }

        GatewayServer server = new GatewayServer(VStore.getInstance());
        System.out.println("Starting py4j gateway...");
        server.start(false);
    }
}
