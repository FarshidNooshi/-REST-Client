package Phase2;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * this class represents a service for the user http requests and stuff.
 */
public class HTTpService {
    private HttpURLConnection connection;
    private Request request;

    /**
     * is the constructor for our service
     *
     * @param req is the request that we wanna execute
     */
    public HTTpService(Request req) {
        request = req;
    }

    /**
     * runs the servcie for the client, e.x. executes the request
     *
     * @throws MalformedURLException for the url connection
     */
    public void runService() throws Exception {
        if (request.getMp().get("help").equals("true"))
            PrintHelp();
        if (!request.getMp().get("data").equals("") && !request.getMp().get("json").equals("")) {
            System.err.println("Program terminated.");
            throw new Exception("can't have both json & form data as message body!");
        }
        URL url = new URL(request.getMp().get("url"));
        try {
            if ("http".equals(url.getProtocol())) {
                connection = (HttpURLConnection) url.openConnection();
            } else if ("https".equals(url.getProtocol())) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                System.err.println("UNSUPPORTED PROTOCOL!");
                return;
            }
            if (request.getMp().get("f").equals("false"))
                connection.setInstanceFollowRedirects(false);
            initHeaders(connection);
            connection.setRequestMethod(request.getMp().get("method"));
            String ret = null;
            switch (connection.getRequestMethod()) {
                case "GET":
                    ret = Get(connection);
                    break;
                case "POST":
                    ret = Post(connection);
                    break;
                case "PATCH":
                    break;
                case "PUT":
                    ret = Put(connection);
                    break;
                case "DELETE":
                    ret = Delete(connection);
                    break;
            }
            System.out.println(ret);
            if (!request.getMp().get("output").equals("")) {
                File file = new File(new File(System.getProperty("user.dir")).getParent() + File.separator + "OutputFolder");
                file.mkdir();
                String name = "output[" + request.getMp().get("output") + "]" + ".txt";
                File actualFile = new File(file, name);
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(actualFile));
                bufferedWriter.write(ret);
                bufferedWriter.flush();
            }
            if (request.getMp().get("i").equals("true")) {
                for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet())
                    System.out.println("Key: " + entry.getKey() + " ,Value: " + entry.getValue() + "\n");
            }
        } catch (IOException ex) {
            System.err.println("FAILED TO OPEN CONNECTION!" + ex);
        }
    }

    /**
     * this method adds the headers of the user input to our connection
     *
     * @param connection is the http connection to the server
     */
    private void initHeaders(HttpURLConnection connection) {
        if (!request.getMp().get("json").equals("")) {
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
        }
        String input = request.getMp().get("headers") + ";";
        int size = input.length();
        for (int i = 0; i < size; i++) {
            String key = null, value = null;
            int j = i;
            while (j < size && input.charAt(j) != ':')
                j++;
            if (j < size)
                key = input.substring(i, j);
            i = ++j;
            while (j < size && input.charAt(j) != ';')
                j++;
            if (i < size)
                value = input.substring(i, j);
            if (value != null && key != null)
                connection.addRequestProperty(key, value);
            i = j;
        }
    }

    private String Get(HttpURLConnection connection) {
        StringBuilder builder = new StringBuilder();
        String line;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            while ((line = in.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return builder.toString();
    }

    private String Post(HttpURLConnection connection) throws IOException {
        connection.setDoOutput(true);
        String data = request.getMp().get("data");
        try (BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream())) {
            out.write(data.getBytes());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (connection.getResponseCode() / 100 == 2)
            return Get(connection);
        return "NOT SUCCESSFUL\n" + connection.getResponseCode() + " " + connection.getResponseMessage();
    }

    private String Put(HttpURLConnection connection) throws IOException {
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            out.write(request.getMp().get("data"));
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (connection.getResponseCode() / 100 == 2)
            return Get(connection);
        return "NOT SUCCESSFUL\n" + connection.getResponseCode() + " " + connection.getResponseMessage();
    }

    private String Delete(HttpURLConnection connection) throws IOException {
        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");
        connection.connect();
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            out.write(request.getMp().get("data"));
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (connection.getResponseCode() / 100 == 2)
            return Get(connection);
        return "NOT SUCCESSFUL\n" + connection.getResponseCode() + " " + connection.getResponseMessage();
    }

    /**
     * this method prints a user guide for the user for how to use the application
     */
    private void PrintHelp() {
        System.out.println("--url: [Address] := address as the request url");
        System.out.println("-M(--method): (GET, POST, PATCH, DELETE, PUT) := request method type");
        System.out.println("-i: tells to print the response headers.");
        System.out.println("-H(--headers): key:value&... := setting the headers of the request");
        System.out.println("-h(--help)  will print the help");
        System.out.println("-f := if typed it will allow the following redirection");
//        System.out.println("");
    }
}