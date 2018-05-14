package edu.mayo.nlp.bsi.uima;

import edu.mayo.bsi.uima.server.api.UIMANLPResultSerializer;
import edu.mayo.bsi.uima.server.api.UIMAStream;
import edu.mayo.bsi.uima.server.core.UIMAServerBase;
import edu.mayo.bsi.uima.server.rest.models.ServerRequest;
import edu.mayo.bsi.uima.server.rest.models.ServerResponse;
import org.apache.uima.cas.CAS;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IntegratedUIMAServer extends UIMAServerBase {
    private Logger logger;

    @Override
    public void start() {
        logger = Logger.getLogger("Integrated-UIMA-REST-Server");
        // Requires nothing special
    }

    public ServerResponse submitJob(ServerRequest req) {
        CompletableFuture<ServerResponse> ret = new CompletableFuture<>();
        if (req.getDocument() == null) {
            ret.completeExceptionally(new IllegalArgumentException("A document must be provided!"));
        }
        if (req.getStreamName() == null) {
            ret.completeExceptionally(new IllegalArgumentException("A stream name must be provided"));
        }
        if (req.getSerializers() == null || req.getSerializers().isEmpty()) {
            ret.completeExceptionally(new IllegalArgumentException("At least 1 deserializer must be defined!"));
        }
        UIMAStream stream = getStream(req.getStreamName().toLowerCase());
        if (stream == null) {
            ret.completeExceptionally(new IllegalArgumentException("There is no currently running stream called " + req.getStreamName()));
        }
        if (!ret.isCompletedExceptionally()) {
            final long startTime = System.currentTimeMillis();
            CompletableFuture<CAS> pipelineResult = stream.submit(req.getDocument(), req.getMetadata());
            pipelineResult.thenApply((cas) -> {
                try {
                    Map<String, String> results = new HashMap<>();
                    for (String serializerName : req.getSerializers()) {
                        UIMANLPResultSerializer serializer = getSerializer(serializerName);
                        if (serializer == null) {
                            results.put(serializerName.toLowerCase(),
                                    "Illegal Argument: serializer " + serializerName.toLowerCase() + " not found!");
                        } else {
                            results.put(serializerName.toLowerCase(), serializer.serializeNLPResult(cas).toString());
                        }
                    }
                    ServerResponse resp = new ServerResponse(System.currentTimeMillis() - startTime,
                            req.getMetadata(), req.getDocument(), results);
                    ret.complete(resp);
                    return cas;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error occurred during pipeline serialization!", e);
                    ret.completeExceptionally(e);
                    return cas;
                }
            });
        }
        try {
            return ret.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, "Error getting CDM Objects!", e);
            return null;
        }
    }
}
