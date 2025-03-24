package com.quad9.aegis.Presenter;


import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteConstraintException;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.TrustedNetwork;
import com.quad9.aegis.Model.TrustedNetworkDbHelper;
import com.quad9.aegis.R;

import java.util.ArrayList;
import java.util.List;

public class TrustedNetworks extends Fragment {
    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;

    public TrustedNetworks() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerView.LayoutManager mLayoutManager;
        View rootView = inflater.inflate(R.layout.fragment_trusted_networks, container, false);

        mRecyclerView = rootView.findViewById(R.id.networks_recycler_view);

        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter();
        mAdapter.updateList();
        mRecyclerView.setAdapter(mAdapter);

        ActivityResultLauncher<String> permissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
                    if (result) {
                        addCurrentNetwork();
                    }
                });

        Button btn = rootView.findViewById(R.id.add_network_btn);
        btn.setOnClickListener(v -> {
            if (
                    ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(null);
                builder.setMessage(R.string.trusted_networks_location_permission);
                LinearLayout layout = new LinearLayout(getActivity());
                layout.setOrientation(LinearLayout.VERTICAL);
                builder.setView(layout);

                builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                });

                builder.create().show();
            } else {
                addCurrentNetwork();
            }
        });

        return rootView;
    }

    private void addCurrentNetwork() {
        WifiManager wifiManager = getSystemService(requireContext(), WifiManager.class);
        try {
            TrustedNetworkDbHelper.getInstance(requireContext()).addTrustedNetwork(new TrustedNetwork(wifiManager.getConnectionInfo()));
            mAdapter.updateList();
            if (DnsSeeker.getStatus().isConnected()) {
                DnsSeeker.getStatus().setOnTrustedNetwork(true);
                DnsSeeker.deActivateService();
            }
        } catch (SQLiteConstraintException ex) {
            // Ignore
        }
    }

    public class MyAdapter extends RecyclerView.Adapter<TrustedNetworks.MyAdapter.MyViewHolder> {
        private List<TrustedNetwork> mDataset = new ArrayList<>();

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class MyViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case

            public TextView domainName;
            public ImageView remove;

            public MyViewHolder(View v) {
                super(v);
                domainName = v.findViewById(R.id.domainName);
                remove = v.findViewById(R.id.removeBtn);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter() {
        }

        // Create new views (invoked by the layout manager)
        @Override
        public TrustedNetworks.MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                         int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_whiltelist, parent, false);

            TrustedNetworks.MyAdapter.MyViewHolder vh = new TrustedNetworks.MyAdapter.MyViewHolder(v);

            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(TrustedNetworks.MyAdapter.MyViewHolder holder, int position) {
            holder.domainName.setText(mDataset.get(position).getSsid());

            holder.remove.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(null);
                builder.setMessage(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_confirm) + mDataset.get(holder.getBindingAdapterPosition()).getSsid() + "?");
                LinearLayout layout = new LinearLayout(getActivity());
                layout.setOrientation(LinearLayout.VERTICAL);
                builder.setView(layout);

                builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_remove), (dialog, id) -> {
                    TrustedNetworkDbHelper.getInstance(requireContext()).removeTrustedNetwork(mDataset.get(holder.getBindingAdapterPosition()));
                    updateList();
                    WifiManager wifiManager = getSystemService(requireContext(), WifiManager.class);
                    try {
                        boolean trusted = TrustedNetworkDbHelper.getInstance(requireContext()).isTrustedNetwork(new TrustedNetwork(wifiManager.getConnectionInfo()));
                        if (DnsSeeker.getStatus().shouldAutoConnect() && !DnsSeeker.getStatus().isConnected() && !trusted) {
                            DnsSeeker.getStatus().setOnTrustedNetwork(false);
                            DnsSeeker.activateService();
                        }
                    } catch (SQLiteConstraintException ex) {
                        // Ignore
                    }
                    dialog.dismiss();
                });
                builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_cancel), (dialog, id) -> {
                });

                builder.create().show();
            });
        }

        public void updateList() {
            mDataset.clear();
            mDataset.addAll(TrustedNetworkDbHelper.getInstance(requireContext()).getTrustedNetworks());
            this.notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
