package com.ride.me.mMassage;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.ride.me.GoridemeAplication;
import com.ride.me.api.ServiceGenerator;
import com.ride.me.api.service.BookService;
import com.ride.me.model.User;
import com.ride.me.model.json.book.massage.GetLayananMassageResponseJson;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by David Studio on 12/21/2017.
 */

public class MenuMassageFragment extends Fragment {

    @BindView(com.ride.me.R.id.menuMassage_recycler)
    RecyclerView recycler;

    @BindView(com.ride.me.R.id.menuMassage_text)
    TextView textView;

    private User loginUser;

    private List<MenuMassageItem> menuItem;
    private FastItemAdapter<MenuMassageItem> adapter;

    private MassageActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof MassageActivity) activity = (MassageActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(com.ride.me.R.layout.fragment_menu_massage, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        Realm realm = GoridemeAplication.getInstance(getActivity()).getRealmInstance();

        menuItem = new ArrayList<>();

        adapter = new FastItemAdapter<>();
        adapter.setNewList(menuItem);
        adapter.notifyDataSetChanged();
        adapter.withSelectable(true);
        adapter.withOnClickListener(new FastAdapter.OnClickListener<MenuMassageItem>() {
            @Override
            public boolean onClick(View v, IAdapter<MenuMassageItem> adapter, MenuMassageItem item, int position) {
                activity.setMassageItem(item);
                activity.addFragmentBackstack(new MassagePreferenceFragment());
                return true;
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        recycler.setAdapter(adapter);

        loginUser = realm.copyFromRealm(GoridemeAplication.getInstance(getActivity()).getLoginUser());

        hideRecycler();

        BookService service = ServiceGenerator.createService(BookService.class, loginUser.getEmail(), loginUser.getPassword());

        service.getLayananMassage().enqueue(new Callback<GetLayananMassageResponseJson>() {
            @Override
            public void onResponse(Call<GetLayananMassageResponseJson> call, Response<GetLayananMassageResponseJson> response) {
                if(response.isSuccessful()) {
                    menuItem = response.body().getData();
                    showRecycler();
                    adapter.setNewList(menuItem);
                    adapter.notifyDataSetChanged();
                } else {
                    hideRecycler();
                }
            }

            @Override
            public void onFailure(Call<GetLayananMassageResponseJson> call, Throwable t) {
                hideRecycler();
            }
        });
    }

    private void showRecycler() {
        recycler.setVisibility(View.VISIBLE);
        textView.setVisibility(View.GONE);
    }

    private void hideRecycler() {
        recycler.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);
    }
}
