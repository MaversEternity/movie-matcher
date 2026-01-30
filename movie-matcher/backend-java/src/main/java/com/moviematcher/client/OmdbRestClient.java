package com.moviematcher.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "omdb-api")
public interface OmdbRestClient {

    @GET
    Uni<OmdbSearchResponse> search(
        @QueryParam("apikey") String apiKey,
        @QueryParam("s") String search,
        @QueryParam("type") String type,
        @QueryParam("page") Integer page,
        @QueryParam("y") Integer year
    );

    @GET
    Uni<OmdbDetailResponse> getDetails(
        @QueryParam("apikey") String apiKey,
        @QueryParam("i") String imdbId,
        @QueryParam("plot") String plot
    );
}
