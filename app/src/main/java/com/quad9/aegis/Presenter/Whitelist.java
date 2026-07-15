package com.quad9.aegis.Presenter;


import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;
import com.quad9.aegis.databinding.CardWhiltelistBinding;
import com.quad9.aegis.databinding.FragmentWhitelistBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class Whitelist extends Fragment {
    private FragmentWhitelistBinding binding;
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

        binding = FragmentWhitelistBinding.inflate(inflater, container, false);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.whitelistRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.whitelistRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter(myDataset);
        binding.whitelistRecyclerView.setAdapter(mAdapter);

        binding.addDomainBtn.setOnClickListener((View.OnClickListener) v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            String title = null;

            builder.setTitle(title);
            builder.setMessage(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_title));
            LinearLayout layout = new LinearLayout(requireActivity());
            layout.setOrientation(LinearLayout.VERTICAL);
            final EditText domain = new EditText(requireActivity());
            domain.setHint(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_hint));

            layout.addView(domain);

            builder.setView(layout);

            builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_add), null);
            builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            final AlertDialog mAlertDialog = builder.create();
            mAlertDialog.setOnShowListener(dialog -> {

                Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(view -> {
                    if (!domain.getText().toString().equals("")) {
                        DnsSeeker.getStatus().addWhitelistDomain(domain.getText().toString());
                        DnsSeeker.popToast(domain.getText() + DnsSeeker.getInstance().getResources().getString(R.string.whitelist_is_added));
                        mAlertDialog.dismiss();
                        mAdapter.updateList();
                    }
                });
            });
            mAlertDialog.show();

        });
        return binding.getRoot();
    }

    public class MyAdapter extends RecyclerView.Adapter<Whitelist.MyAdapter.MyViewHolder> {
        private List<String> mDataset;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class MyViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case

            private CardWhiltelistBinding binding;

            public MyViewHolder(View v) {
                super(v);
                binding = CardWhiltelistBinding.bind(v);
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
            holder.binding.domainName.setText(domain);

/*
            holder.whitelist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DnsSeeker.getStatus().addWhitelistDomain(mDataset.get(position));
                    DnsSeeker.popToast(mDataset.get(position) + " is added in domain whitelist.");
                }
            });*/

            if (DnsSeeker.getStatus().isInStaticWhitelistDomains(domain)) {
                holder.binding.removeBtn.setVisibility(View.GONE);
            } else {
                holder.binding.removeBtn.setVisibility(View.VISIBLE);
            }

            holder.binding.removeBtn.setOnClickListener((View.OnClickListener) v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                String title = null;

                builder.setTitle(title);
                builder.setMessage(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_confirm) + "\"" + domain + "\"?");
                LinearLayout layout = new LinearLayout(requireActivity());
                layout.setOrientation(LinearLayout.VERTICAL);
                builder.setView(layout);

                builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_remove), null);
                builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                final AlertDialog mAlertDialog = builder.create();
                mAlertDialog.setOnShowListener(dialog -> {

                    Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    b.setOnClickListener(view -> {
                        DnsSeeker.getStatus().removeWhitelistDomain(domain);
                        updateList();
                        mAlertDialog.dismiss();
                    });
                });
                mAlertDialog.show();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
