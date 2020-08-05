package com.anand.musictok.SoundLists;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.anand.musictok.Main_Menu.RelateToFragment_OnBack.RootFragment;
import com.anand.musictok.R;
import com.anand.musictok.SimpleClasses.ApiRequest;
import com.anand.musictok.SimpleClasses.Callback;
import com.anand.musictok.SimpleClasses.Functions;
import com.anand.musictok.SimpleClasses.Variables;
import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.downloader.request.DownloadRequest;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

public class Discover_SoundList_F extends RootFragment implements Player.EventListener{

    RecyclerView listview;
    Sounds_Adapter adapter;
    ArrayList<Sound_catagory_Get_Set> datalist;

    DownloadRequest prDownloader;
    static boolean active = false;

    View view;
    Context context;

    SwipeRefreshLayout swiperefresh;
    ProgressBar pbar;

    public static String running_sound_id;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view= inflater.inflate(R.layout.activity_sound_list, container, false);
        context=getContext();

        running_sound_id="none";


        PRDownloader.initialize(context);
        pbar=view.findViewById(R.id.pbar);

        datalist=new ArrayList<>();

        listview = view.findViewById(R.id.listview);
        listview.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listview.setNestedScrollingEnabled(false);
        listview.setHasFixedSize(true);
        listview.getLayoutManager().setMeasurementCacheEnabled(false);


        swiperefresh=view.findViewById(R.id.swiperefresh);
        swiperefresh.setColorSchemeResources(R.color.black);
        swiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                previous_url="none";
                StopPlaying();
                Call_Api_For_get_allsound();
            }
        });

        Call_Api_For_get_allsound();

        return view;
    }



    public void Set_adapter(){

        adapter=new Sounds_Adapter(context, datalist, new Sounds_Adapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view,int postion, Sounds_GetSet item) {

                Log.d("resp",item.acc_path);

                if(view.getId()==R.id.done){
                    StopPlaying();
                    Down_load_mp3(item.id,item.sound_name,item.acc_path);
                }
               else if(view.getId()==R.id.fav_btn){
                    Call_Api_For_Fav_sound(postion, item);
                }
                else {
                    if (thread != null && !thread.isAlive()) {
                        StopPlaying();
                        playaudio(view, item);
                    } else if (thread == null) {
                        StopPlaying();
                        playaudio(view, item);
                    }
                }

            }
        });

        listview.setAdapter(adapter);


    }




    private void Call_Api_For_get_allsound() {

        JSONObject parameters = new JSONObject();
        try {
            parameters.put("fb_id", Variables.sharedPreferences.getString(Variables.u_id,"0"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("resp",parameters.toString());

        ApiRequest.Call_Api(context, Variables.allSounds, parameters, new Callback() {
            @Override
            public void Responce(String resp) {
                swiperefresh.setRefreshing(false);
                pbar.setVisibility(View.GONE);
                Parse_data(resp);
            }
        });


    }



    public void Parse_data(String responce){

        datalist=new ArrayList<>();

        try {
            JSONObject jsonObject=new JSONObject(responce);
            String code=jsonObject.optString("code");
            if(code.equals("200")){

                JSONArray msgArray=jsonObject.getJSONArray("msg");

                for(int i=msgArray.length()-1;i>=0;i--) {
                    JSONObject object=msgArray.getJSONObject(i);

                    Log.d("resp",object.toString());

                    JSONArray section_array=object.optJSONArray("sections_sounds");

                    ArrayList<Sounds_GetSet> sound_list=new ArrayList<>();

                    for (int j = 0; j < section_array.length(); j++) {
                        JSONObject itemdata = section_array.optJSONObject(j);

                        Sounds_GetSet item = new Sounds_GetSet();

                        item.id = itemdata.optString("id");

                        JSONObject audio_path = itemdata.optJSONObject("audio_path");
                        item.acc_path = audio_path.optString("acc");


                        item.sound_name = itemdata.optString("sound_name");
                        item.description = itemdata.optString("description");
                        item.section = itemdata.optString("section");
                        String thum_image=itemdata.optString("thum");

                        if(thum_image!=null && thum_image.contains("http"))
                        item.thum = itemdata.optString("thum");
                        else
                            item.thum = Variables.base_url+itemdata.optString("thum");

                        item.date_created = itemdata.optString("created");
                        item.fav=itemdata.optString("fav");

                        sound_list.add(item);
                    }

                    Sound_catagory_Get_Set sound_catagory_get_set = new Sound_catagory_Get_Set();
                    sound_catagory_get_set.catagory = object.optString("section_name");
                    sound_catagory_get_set.sound_list = sound_list;

                    datalist.add(sound_catagory_get_set);

                }


                Set_adapter();


            }else {
                Toast.makeText(context, ""+jsonObject.optString("msg"), Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {

            e.printStackTrace();
        }

    }



    @Override
    public boolean onBackPressed() {
        getActivity().onBackPressed();
        return super.onBackPressed();
    }





    View previous_view;
    Thread thread;
    SimpleExoPlayer player;
    String previous_url="none";
    public void playaudio(View view, final Sounds_GetSet item){
        previous_view=view;

        if(previous_url.equals(item.acc_path)){

            previous_url="none";
            running_sound_id="none";
        }else {

            previous_url=item.acc_path;
            running_sound_id=item.id;

            DefaultTrackSelector trackSelector = new DefaultTrackSelector();
            player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);

            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                    Util.getUserAgent(context, "TikTok"));

            MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(item.acc_path));


            player.prepare(videoSource);
            player.addListener(this);


            player.setPlayWhenReady(true);



        }

    }


    public void StopPlaying(){
        if(player!=null){
            player.setPlayWhenReady(false);
            player.removeListener(this);
            player.release();
        }

        show_Stop_state();

    }



  @Override
    public void onStart() {
        super.onStart();
        active=true;
    }



    @Override
    public void onStop() {
        super.onStop();
        active=false;

        running_sound_id="null";

        if(player!=null){
            player.setPlayWhenReady(false);
            player.removeListener(this);
            player.release();
        }

        show_Stop_state();

    }



    public void Show_Run_State(){

    if (previous_view != null) {
        previous_view.findViewById(R.id.loading_progress).setVisibility(View.GONE);
        previous_view.findViewById(R.id.pause_btn).setVisibility(View.VISIBLE);
        previous_view.findViewById(R.id.done).setVisibility(View.VISIBLE);
    }

    }


    public void Show_loading_state(){
        previous_view.findViewById(R.id.play_btn).setVisibility(View.GONE);
        previous_view.findViewById(R.id.loading_progress).setVisibility(View.VISIBLE);
    }


    public void show_Stop_state(){

        if (previous_view != null) {
            previous_view.findViewById(R.id.play_btn).setVisibility(View.VISIBLE);
            previous_view.findViewById(R.id.loading_progress).setVisibility(View.GONE);
            previous_view.findViewById(R.id.pause_btn).setVisibility(View.GONE);
            previous_view.findViewById(R.id.done).setVisibility(View.GONE);
        }

        running_sound_id="none";

    }



    public void Down_load_mp3(final String id,final String sound_name, String url){

        final ProgressDialog progressDialog=new ProgressDialog(context);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

        prDownloader= PRDownloader.download(url, Variables.app_folder, Variables.SelectedAudio_AAC)
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {

                    }
                })
                .setOnPauseListener(new OnPauseListener() {
                    @Override
                    public void onPause() {

                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {

                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {

                    }
                });

        prDownloader.start(new OnDownloadListener() {
            @Override
            public void onDownloadComplete() {
                progressDialog.dismiss();
                Intent output = new Intent();
                output.putExtra("isSelected","yes");
                output.putExtra("sound_name",sound_name);
                output.putExtra("sound_id",id);
                getActivity().setResult(RESULT_OK, output);
                getActivity().finish();
                getActivity().overridePendingTransition(R.anim.in_from_top, R.anim.out_from_bottom);
            }

            @Override
            public void onError(Error error) {
                progressDialog.dismiss();
            }
        });

    }



    private void Call_Api_For_Fav_sound(int pos, final Sounds_GetSet item) {

       JSONObject parameters = new JSONObject();
        try {
            parameters.put("fb_id", Variables.sharedPreferences.getString(Variables.u_id,"0"));
            parameters.put("sound_id",item.id);
            if(item.fav.equals("1"))
            parameters.put("fav","0");
            else
                parameters.put("fav","1");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Functions.Show_loader(context,false,false);
        ApiRequest.Call_Api(context, Variables.fav_sound, parameters, new Callback() {
            @Override
            public void Responce(String resp) {
                Functions.cancel_loader();

                if(item.fav.equals("1"))
                    item.fav="0";
                else
                   item.fav="1";

                for (int i=0;i<datalist.size();i++){
                    Sound_catagory_Get_Set catagory_get_set=datalist.get(i);
                    if(catagory_get_set.sound_list.contains(item)){
                        int index= catagory_get_set.sound_list.indexOf(item);
                        catagory_get_set.sound_list.remove(item);
                        catagory_get_set.sound_list.add(index,item);
                        break;
                    }
                }

                adapter.notifyDataSetChanged();

            }
        });

    }


    @Override
    public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        if(playbackState==Player.STATE_BUFFERING){
           Show_loading_state();
        }
        else if(playbackState==Player.STATE_READY){
            Show_Run_State();
        }else if(playbackState==Player.STATE_ENDED){
            show_Stop_state();
        }

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }


    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }


    @Override
    public void onPositionDiscontinuity(int reason) {

    }


    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }


    @Override
    public void onSeekProcessed() {

    }




}
