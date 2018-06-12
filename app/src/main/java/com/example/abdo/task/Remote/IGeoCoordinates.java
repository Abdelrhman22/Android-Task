package com.example.abdo.task.Remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IGeoCoordinates {


    @GET("maps/api/directions/json")
    Call <String> getDirections (@Query("origin") String origin , @Query("destination") String destination);
}
