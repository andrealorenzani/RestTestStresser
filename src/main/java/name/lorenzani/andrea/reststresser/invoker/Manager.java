package name.lorenzani.andrea.reststresser.invoker;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Manager {

    private ExecutorService pool = Executors.newFixedThreadPool(5);
    private final String restQuery;
    private final int startNumThread;
    private final CloseableHttpClient httpclient;
    private PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

    public Manager(String query, int startNumThread) {
        cm.setMaxTotal(startNumThread * 10);
        httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        restQuery = query;
        this.startNumThread = startNumThread;
    }

    public void stress(){
        int currNumThread = startNumThread;
        for(int i=0; i<10; i++){
            pool = Executors.newFixedThreadPool(Math.min(32, currNumThread));
            long start = System.currentTimeMillis();
            List<CompletableFuture<String>> threads = IntStream.rangeClosed(1, currNumThread)
                    .boxed()
                    .map(this::invokeRest)
                    .collect(Collectors.toList());
            CompletableFuture<List<String>> res = CompletableFuture.supplyAsync(() -> threads.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));
            try{
                Thread.sleep(10000 - (System.currentTimeMillis()-start));
            }catch (InterruptedException ex) {
                System.err.println("Cannot wait for 10 seconds: "+ex.getMessage());
            }
            List<String> errors = res.join()
                    .stream()
                    .filter(s -> s.startsWith("ERROR"))
                    .map(s -> s.substring(6))
                    .collect(Collectors.toList());
            System.out.println(String.format("Invoking REST %d times with %d errors in %d millis", currNumThread, errors.size(), (System.currentTimeMillis()-start)));
            if(errors.size() > 0) {
                printStats(currNumThread, errors);
            }
            currNumThread = (2 + i) * startNumThread;
        }
    }

    private void printStats(int numTest, List<String> errors) {
        HashMap<String, Integer> occurrences = new HashMap<>();
        errors.forEach(err -> {
            occurrences.putIfAbsent(err, 0);
            occurrences.put(err, occurrences.get(err)+1);
        });
        occurrences.forEach((err, occ) -> System.out.println(String.format("Error '%s' occurred in %s of requests", err, ""+((100*occ)/numTest)+"%")));
    }

    private CompletableFuture<String> invokeRest(final int i){
        return CompletableFuture.supplyAsync(() -> {
            HttpPost req = new HttpPost(restQuery);
            String json = "{ \"class\": \"barbarian\", \"level\":1, \"race\": \"dwarf\"}";
            req.setHeader("Accept", "application/json");
            req.setHeader("Content-type", "application/json");
            try{
                req.setEntity(new StringEntity(json));
            } catch (Exception e) {}

            try (CloseableHttpResponse response = httpclient.execute(req, new BasicHttpContext())) {
                // get the response body as an array of bytes
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return new String(EntityUtils.toByteArray(entity));
                }
                return null;
            }
            catch (Exception e) {
                return String.format("ERROR %s", e.getMessage());
            }
        }, pool);
    }
}
