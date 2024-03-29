package com.example.finalproject.ui.usersearch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.opengl.Visibility;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.WifiDirectBroadcastReceiver;
import com.example.finalproject.ui.MainActivity;
import com.example.finalproject.ui.models.HistoryModel;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import pl.droidsonroids.gif.GifImageView;


import static android.os.Looper.getMainLooper;

public class FindUserFragment extends Fragment implements UserSearchContract.View{

    private ConstraintLayout searchLayout;
    private RecyclerView recyclerView;
    private UserSearchRecycleViewAdapter adapter;
    private UserSearchPresenter presenter;

    private TextView loadingText;

    private ImageView radioImg;
    private GifImageView radioGif;

    public FindUserFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.find_user_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((NavigationView)getActivity().findViewById(R.id.navigation)).setCheckedItem(R.id.open_chat);

        getActivity().findViewById(R.id.toolbar).setVisibility(View.GONE);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        ((MainActivity)getActivity()).enableToggle();

        TextView title = getActivity().findViewById(R.id.toolbar_title);
        title.setText("");

        loadingText = getActivity().findViewById(R.id.loading_text);

        radioImg = getActivity().findViewById(R.id.radio_img);
        radioGif = getActivity().findViewById(R.id.radio_gif);

        radioImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.setupDiscover();
                setDiscovery(true);
            }
        });


        searchLayout = view.findViewById(R.id.user_search_layout);

        view.findViewById(R.id.cancel_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.main_fragment);
                navController.navigate(R.id.action_findUserFragment_to_historyFragment, null);
            }
        });


        this.recyclerView = view.findViewById(R.id.user_search_recycle_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        this.adapter = new UserSearchRecycleViewAdapter(this);
        recyclerView.setAdapter(adapter);

        ImageView deleteButton = getActivity().findViewById(R.id.delete_button);
        deleteButton.setVisibility(View.GONE);

        getActivity().findViewById(R.id.toolbar_subtitle).setVisibility(View.GONE);
        ((TextView) getActivity().findViewById(R.id.toolbar_title)).setText("მომხმარებლები");

        this.presenter = new UserSearchPresenter(this, getActivity());
    }



    @Override
    public void onResume() {
        super.onResume();
        presenter.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    public void showData(List<HistoryModel> list) {
        getActivity().findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        adapter.setItems(list);
    }

    @Override
    public void setDiscovery(boolean on) {
        if (on) {
            radioImg.setVisibility(View.GONE);
            radioGif.setVisibility(View.VISIBLE);
        } else {
            radioImg.setVisibility(View.VISIBLE);
            radioGif.setVisibility(View.GONE);
        }
    }

    @Override
    public void chatClicked(HistoryModel model) {
        presenter.chatClicked(model);
    }

    public static String SEARCHING_DEVICES = "მომხმარებლების ძებნა...";
    public static String CONNECTING_TO = "მიმდინარეობს დაკავშირება";

    @Override
    public void showProgressBar(String text) {
        if (!text.isEmpty())
            loadingText.setText(text);
        searchLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar() {
        searchLayout.setVisibility(View.GONE);
    }
}
