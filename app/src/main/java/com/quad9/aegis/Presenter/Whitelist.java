package com.quad9.aegis.Presenter;


import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class Whitelist extends Fragment {
    private int debugCounter = 0;
    private View rootView;
    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;


    public Whitelist() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
// TODO Auto-generated method stub
        RecyclerView.LayoutManager mLayoutManager;
        List<String> myDataset = new ArrayList<>(DnsSeeker.getStatus().getWhitelistDomain());
        Collections.sort(myDataset);

        //blocked = DnsSeeker.getStatus().recentBlocking();
        Log.d("Record", "view creating");
        rootView = inflater.inflate(R.layout.fragment_whitelist, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.whitelist_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);

        Button btn = rootView.findViewById(R.id.add_domain_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                String title = null;

                builder.setTitle(title);
                builder.setMessage(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_title));
                LinearLayout layout = new LinearLayout(getActivity());
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText domain = new EditText(getActivity());
                //input.setText("Email");
                domain.setHint(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_hint));

                layout.addView(domain);

                builder.setView(layout);

                builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_add), null);
                builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                final AlertDialog mAlertDialog = builder.create();
                mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(DialogInterface dialog) {

                        Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        b.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View view) {
                                if (!domain.getText().toString().equals("")) {
                                    DnsSeeker.getStatus().addWhitelistDomain(domain.getText().toString());
                                    DnsSeeker.popToast(domain.getText() + DnsSeeker.getInstance().getResources().getString(R.string.whitelist_is_added));
                                    mAlertDialog.dismiss();
                                    mAdapter.updateList();
                                }
                            }
                        });
                    }
                });
                mAlertDialog.show();

            }
        });
        return rootView;
    }

    private List<String> getData() {
        List<String> data = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            data.add(i, "" + i);
        }
        return data;
    }

    public class MyAdapter extends RecyclerView.Adapter<Whitelist.MyAdapter.MyViewHolder> {
        private List<String> mDataset;
        private int mExpandedPosition = -1;

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
        public MyAdapter(List<String> myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public Whitelist.MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                   int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_whiltelist, parent, false);

            Whitelist.MyAdapter.MyViewHolder vh = new Whitelist.MyAdapter.MyViewHolder(v);

            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(Whitelist.MyAdapter.MyViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            String domain = mDataset.get(position);
            holder.domainName.setText(domain);

/*
            holder.whitelist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DnsSeeker.getStatus().addWhitelistDomain(mDataset.get(position));
                    DnsSeeker.popToast(mDataset.get(position) + " is added in domain whitelist.");
                }
            });*/

            if (DnsSeeker.getStatus().isInStaticWhitelistDomains(domain)) {
                holder.remove.setVisibility(View.GONE);
            } else {
                holder.remove.setVisibility(View.VISIBLE);
            }

            holder.remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    String title = null;

                    builder.setTitle(title);
                    builder.setMessage(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_confirm) + "\"" + domain + "\"?");
                    LinearLayout layout = new LinearLayout(getActivity());
                    layout.setOrientation(LinearLayout.VERTICAL);
                    builder.setView(layout);

                    builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_remove), null);
                    builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

                    final AlertDialog mAlertDialog = builder.create();
                    mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                        @Override
                        public void onShow(DialogInterface dialog) {

                            Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            b.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View view) {
                                    DnsSeeker.getStatus().removeWhitelistDomain(domain);
                                    updateList();
                                    mAlertDialog.dismiss();
                                }

                            });
                        }
                    });
                    mAlertDialog.show();
                }
            });
        }

        public void updateList() {
            List<String> myDataset = new ArrayList<>(DnsSeeker.getStatus().getWhitelistDomain());
            Collections.sort(myDataset);
            mDataset.clear();
            mDataset.addAll(myDataset);
            this.notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
