package com.crowdmeter.Utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPreferences {

    public static SharedPreferences sp;

    public static SharedPreferences createSharedPref(Context cont){
        if(sp==null)
            sp=cont.getSharedPreferences("MassageOnDemand",Context.MODE_PRIVATE);

        return sp;
    }

    public static String getAddress(Context cont){
        createSharedPref(cont);
        return sp.getString("address", null);
    }

    public static void setAddress(Context cont, String myAdd){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("address", myAdd);
        spe.apply();
    }


    // get and setPollQues stores current question for the tile which is selected when user clicks one of the tiles.
    public static String getPollQues(Context cont){
        createSharedPref(cont);
        return sp.getString("MyCODE", null);
    }
    public static void setPollQues(Context cont, String id){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("MyCODE", id);
        spe.apply();
    }


    // set and getHasPolled to check whether a user has already polled or not
    public static boolean getHasPolled(Context cont){
        createSharedPref(cont);
        return sp.getBoolean("isPolled", false);
    }
    public static void setHasPolled(Context cont, Boolean id){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        spe.putBoolean("isPolled", id);
        spe.apply();
    }

    // when a new poll is added -> set true to retreive the new tiles
    public static boolean getisNewPoll(Context cont){
        createSharedPref(cont);
        return sp.getBoolean("newpoll", false);
    }
    public static void setisNewPoll(Context cont, Boolean id){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        spe.putBoolean("newpoll", id);
        spe.apply();
    }


    // stores all the options for any question

    public static List<String> getOptionsList(Context cont){
        createSharedPref(cont);
        Gson gson = new Gson();
        String json = sp.getString("AddressList",null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> addresslist = gson.fromJson(json,type);
        if(addresslist==null){
            addresslist = new ArrayList<>();
        }
        return addresslist;
    }
    public static void setOptionssList(Context cont, List<String> addresslist){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        Gson gson = new Gson();
        String json = gson.toJson(addresslist);
        spe.putString("AddressList", json);
        spe.apply();
    }



    // store pollquestion as key and its respective options as values

    public static Map<String,List<String>> getAllPolls(Context cont){
        createSharedPref(cont);
        Gson gson = new Gson();
        String json = sp.getString("allpolls",null);
        Type type = new TypeToken<HashMap<String,List<String>>>() {}.getType();
        Map<String,List<String>> polllist = gson.fromJson(json,type);
        if(polllist==null){
            polllist = new HashMap<>();
        }
        return polllist;
    }
    public static void setAllPolls(Context cont, Map<String,List<String>> polllist){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        Gson gson = new Gson();
        String json = gson.toJson(polllist);
        spe.putString("allpolls", json);
        spe.apply();
    }


    // stores pollquestion as key and title as value
    public static Map<String,String> getTitle(Context cont){
        createSharedPref(cont);
        Gson gson = new Gson();
        String json = sp.getString("titles",null);
        Type type = new TypeToken<HashMap<String,String>>() {}.getType();
        Map<String,String> titlelist = gson.fromJson(json,type);
        if(titlelist==null){
            titlelist = new HashMap<>();
        }
        return titlelist;
    }

    public static void setTitle(Context cont, Map<String,String> titlelist){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        Gson gson = new Gson();
        String json = gson.toJson(titlelist);
        spe.putString("titles", json);
        spe.apply();
    }


    public static void clearSP() {
        sp.edit().clear().apply();
    }


    //store user's current latitude
    public static String getLat(Context cont){
        createSharedPref(cont);
        return sp.getString("latitude", null);
    }

    public static void setLat(Context cont, String lat){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("latitude", lat);
        spe.apply();
    }


    //store user's current longitude
    public static String getLon(Context cont){
        createSharedPref(cont);
        return sp.getString("longitude", null);
    }

    public static void setLon(Context cont, String lon){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("longitude", lon);
        spe.apply();
    }



    // store all lat-lon values for any poll question after retrieval from firebase
    public static List<LatLng> getAllLatLng(Context cont){
        createSharedPref(cont);
        Gson gson = new Gson();
        String json = sp.getString("latlng",null);
        Type type = new TypeToken<ArrayList<LatLng>>() {}.getType();

        List<LatLng> addresslist = gson.fromJson(json,type);
        if(addresslist==null){
            addresslist = new ArrayList<>();
        }
        return addresslist;
    }

    public static void setAllLatLng(Context cont, List<LatLng> addresslist){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        Gson gson = new Gson();
        String json = gson.toJson(addresslist);
        spe.putString("latlng", json);
        spe.apply();
    }


    // store all the responses(yes or no) for any question after retrieval from firebase
    public static List<String> getResponseList(Context cont){
        createSharedPref(cont);
        Gson gson = new Gson();
        String json = sp.getString("ResponseList",null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> responselist = gson.fromJson(json,type);
        if(responselist==null){
            responselist = new ArrayList<>();
        }
        return responselist;
    }

    public static void setResponseList(Context cont, List<String> responselist){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        Gson gson = new Gson();
        String json = gson.toJson(responselist);
        spe.putString("ResponseList", json);
        spe.apply();
    }


    // store user's chosen option when casting a vote

    public static String getChosenOption(Context cont){
        createSharedPref(cont);
        return sp.getString("myopt", null);
    }
    public static void setChosenOption(Context cont, String id){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("myopt", id);
        spe.apply();
    }



    // store title list of all questions
    public static List<String> getTitleList(Context cont){
        createSharedPref(cont);
        Gson gson = new Gson();
        String json = sp.getString("TitleList",null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> titlelist = gson.fromJson(json,type);
        if(titlelist==null){
            titlelist = new ArrayList<>();
        }
        return titlelist;
    }

    public static void setTitleList(Context cont, List<String> titlelist){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        Gson gson = new Gson();
        String json = gson.toJson(titlelist);
        spe.putString("TitleList", json);
        spe.apply();
    }

    public static Map<String,String> getTitleQuestion(Context cont){
        createSharedPref(cont);
        Gson gson = new Gson();
        String json = sp.getString("titleQues",null);
        Type type = new TypeToken<HashMap<String,String>>() {}.getType();
        Map<String,String> titlelist = gson.fromJson(json,type);
        if(titlelist==null){
            titlelist = new HashMap<>();
        }
        return titlelist;
    }

    public static void setTitleQuestion(Context cont, Map<String,String> titlelist){
        createSharedPref(cont);
        SharedPreferences.Editor spe = sp.edit();
        Gson gson = new Gson();
        String json = gson.toJson(titlelist);
        spe.putString("titleQues", json);
        spe.apply();
    }



}
