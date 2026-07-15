package com.quad9.aegis.Presenter;


import static com.quad9.aegis.Model.GlobalVariables.ALL;
import static com.quad9.aegis.Model.GlobalVariables.BLOCKED;
import static com.quad9.aegis.Model.GlobalVariables.FAILED;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.ResponseRecord;
import com.quad9.aegis.R;
import com.quad9.aegis.databinding.DnsRecordBinding;
import com.quad9.aegis.databinding.FragmentRecordBinding;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class Record extends Fragment {
    private static final String TAG = "Record";

    private FragmentRecordBinding binding;
    private RecyclerView.Adapter<MyAdapter.MyViewHolder> mAdapter;

    public Record() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Bundle bundle = getArguments();
        int blocked = bundle.getInt("isBlocked");
        binding = FragmentRecordBinding.inflate(inflater, container, false);
        prepareData(blocked);
        if (blocked == BLOCKED) {
            binding.dnsFilter.check(R.id.blocked);
        } else if (blocked == FAILED) {
            binding.dnsFilter.check(R.id.failed);
        }
        binding.dnsFilter.setOnCheckedChangeListener(listener);
        return binding.getRoot();
    }

    public void prepareData(int isblocked) {
        RecyclerView.LayoutManager mLayoutManager;
        List<ResponseRecord> myDataset;
        if (isblocked == BLOCKED) {
            myDataset = DnsSeeker.getInstance().getBlocked();
            if (myDataset.isEmpty()) {
                binding.textEmpty.setText(DnsSeeker.getInstance().getResources().getString(R.string.empty_recent_blocked));
                binding.textEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.textEmpty.setVisibility(View.INVISIBLE);
            }
        } else if (isblocked == ALL) {
            myDataset = DnsSeeker.getInstance().getResponse();
            if (myDataset.isEmpty()) {
                binding.textEmpty.setText(DnsSeeker.getInstance().getResources().getString(R.string.empty_recent_query));
                binding.textEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.textEmpty.setVisibility(View.INVISIBLE);
            }
        } else {
            myDataset = DnsSeeker.getInstance().getFailedResponse();
            if (myDataset.isEmpty()) {
                binding.textEmpty.setText(DnsSeeker.getInstance().getResources().getString(R.string.empty_recent_failed));
                binding.textEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.textEmpty.setVisibility(View.INVISIBLE);
            }
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.myRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.myRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter(myDataset);
        binding.myRecyclerView.setAdapter(mAdapter);
    }

    private RadioGroup.OnCheckedChangeListener listener = new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // TODO Auto-generated method stub
            switch (checkedId) {
                case R.id.all:
                    prepareData(ALL);
                    mAdapter.notifyDataSetChanged();
                    break;
                case R.id.blocked:
                    prepareData(BLOCKED);
                    mAdapter.notifyDataSetChanged();
                    break;
                case R.id.failed:
                    prepareData(FAILED);
                    mAdapter.notifyDataSetChanged();
                    break;
            }

        }

    };

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        private List<ResponseRecord> mDataset;
        private int mExpandedPosition = -1;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class MyViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public DnsRecordBinding binding;

            public MyViewHolder(View v) {
                super(v);
                binding = DnsRecordBinding.bind(v);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<ResponseRecord> myDataset) {
            Log.d(TAG, "WTF?");
            mDataset = myDataset;
            binding.myRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent,
                                               int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dns_record, parent, false);

            MyViewHolder vh = new MyViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(Record.MyAdapter.MyViewHolder holder, int dummy) {
            int position = holder.getAdapterPosition();
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            ResponseRecord r = mDataset.get(position);
            Log.d("recyclerview", r.name);
            holder.binding.domainName.setText(r.name);
            holder.binding.ip.setText(DnsSeeker.getInstance().getResources().getString(R.string.record_ans, r.IP));
            holder.binding.timeDes.setText(DnsSeeker.getInstance().getResources().getString(R.string.record_query_time, r.time));
            holder.binding.type.setText(DnsSeeker.getInstance().getResources().getString(R.string.record_type, r.type));
            holder.binding.timeStamp.setText(r.timeStamp);
            int cardBgText = DnsSeeker.getInstance().getResources().getColor(R.color.cardBgText);
            int cardMaliText = DnsSeeker.getInstance().getResources().getColor(R.color.success);
            int cardFailedText = DnsSeeker.getInstance().getResources().getColor(R.color.white);
            final boolean isExpanded = position == mExpandedPosition;
            if (r.IP.equals("MALICIOUS")) {
                holder.binding.cvInner.setCardBackgroundColor(DnsSeeker.getInstance().getResources().getColor(R.color.theButton));
                holder.binding.ip.setTextColor(cardMaliText);
                holder.binding.type.setTextColor(cardMaliText);
                holder.binding.domainName.setTextColor(cardMaliText);
                holder.binding.iconResult.setImageResource(R.drawable.ic_block_white_24dp);
                holder.binding.iconResult.setColorFilter(cardMaliText);
                holder.binding.timeDes.setTextColor(cardMaliText);
                holder.binding.timeStamp.setTextColor(cardMaliText);
                holder.binding.whitelistBtn.setBackground(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.rounded_corner_malicious));
                holder.binding.whitelistBtn.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

                if (r.getProviders() != null && r.getProvidersUrl() != null) {
                    String temp = "Provider: " + listToString(r.getProviders(), r.getProvidersUrl());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
                        // we are using this flag to give a consistent behaviour
                        holder.binding.provider.setText(Html.fromHtml(temp, Html.FROM_HTML_MODE_COMPACT));

                    } else {
                        holder.binding.provider.setText(Html.fromHtml(temp));
                    }
                    holder.binding.provider.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.binding.provider.setClickable(true);

                }
                holder.binding.provider.setTextColor(cardMaliText);
                holder.binding.provider.setLinkTextColor(cardMaliText);

                holder.binding.provider.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            } else if (r.IP.equals("TIMEOUT") || r.IP.equals("No Network Available") || r.IP.equals("SEND_FAIL")) {
                holder.binding.cvInner.setCardBackgroundColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.binding.ip.setTextColor(cardBgText);
                holder.binding.type.setTextColor(cardBgText);
                holder.binding.domainName.setTextColor(cardBgText);
                holder.binding.iconResult.setImageResource(R.drawable.ic_clear_white_24dp);
                holder.binding.timeDes.setTextColor(cardBgText);
                holder.binding.timeStamp.setTextColor(cardBgText);
                holder.binding.whitelistBtn.setBackground(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.rounded_corner_gray));
                holder.binding.whitelistBtn.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                holder.binding.provider.setVisibility(View.GONE);
            } else {
                holder.binding.cvInner.setCardBackgroundColor(DnsSeeker.getInstance().getResources().getColor(R.color.success));
                holder.binding.ip.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.binding.type.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.binding.domainName.setTextColor(Color.BLACK);
                holder.binding.timeStamp.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.binding.timeDes.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.binding.iconResult.setImageResource(R.drawable.ic_check_black_24dp);

                holder.binding.whitelistBtn.setBackground(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.rounded_corner_gray));
                holder.binding.provider.setVisibility(View.GONE);
                holder.binding.whitelistBtn.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            }

            holder.binding.theExpandArea.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.itemView.setActivated(isExpanded);
            holder.itemView.setOnClickListener(v -> {
                mExpandedPosition = isExpanded ? -1 : position;
                TransitionManager.beginDelayedTransition(binding.myRecyclerView);
                //notifyDataSetChanged();
                notifyItemChanged(position);
            });
            holder.itemView.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) DnsSeeker.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Dns Record", r.toString());
                clipboard.setPrimaryClip(clip);
                DnsSeeker.popToast("Add to clipboard");
                return true;
            });
            holder.binding.whitelistBtn.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                builder.setTitle(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_dialogue_title));
                builder.setMessage(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_dialogue_content));

                builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_dialogue_proceed), null);
                builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_dialogue_cancel), (dialog, id) -> {
                });
                final AlertDialog mAlertDialog = builder.create();
                mAlertDialog.show();
                mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((z -> {
                    DnsSeeker.getStatus().addWhitelistDomain(r.name);
                    DnsSeeker.popToast(r.name + DnsSeeker.getInstance().getResources().getString(R.string.whitelist_is_added));
                    mAlertDialog.dismiss();
                }));

            });
        }


        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            if (mDataset == null)
                return 0;
            return mDataset.size();
        }
    }

    private String listToString(List<String> l, List<String> url) {
        String str = "";
        for (int i = 0; i < l.size(); i++) {
            if (url.get(i).equals("")) {
                str += "<br>" + l.get(i);
            } else {
                str += "<br>" + "<a href='" + url.get(i) + "'> " + l.get(i) + " </a>";
            }
        }
        return str;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
