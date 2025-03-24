package com.quad9.aegis.Presenter;


import static com.quad9.aegis.Model.GlobalVariables.ALL;
import static com.quad9.aegis.Model.GlobalVariables.BLOCKED;
import static com.quad9.aegis.Model.GlobalVariables.FAILED;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.ResponseRecord;
import com.quad9.aegis.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class Record extends Fragment {
    private static final String TAG = "Record";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    View rootView;
    private RadioGroup rgroup;
    TextView text_empty;

    public Record() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Bundle bundle = getArguments();
        int blocked = bundle.getInt("isBlocked");
        //blocked = DnsSeeker.getStatus().recentBlocking();
        rootView = inflater.inflate(R.layout.fragment_record, container, false);
        text_empty = rootView.getRootView().findViewById(R.id.text_empty);
        prepareData(blocked);
        rgroup = (RadioGroup) rootView.findViewById(R.id.dns_filter);
        if (blocked == BLOCKED) {
            rgroup.check(R.id.blocked);
        } else if (blocked == FAILED) {
            rgroup.check(R.id.failed);
        }
        rgroup.setOnCheckedChangeListener(listener);
        return rootView;
    }

    public void prepareData(int isblocked) {
        RecyclerView.LayoutManager mLayoutManager;
        List<ResponseRecord> myDataset;
        if (isblocked == BLOCKED) {
            myDataset = DnsSeeker.getInstance().getBlocked();
            if (myDataset.isEmpty()) {
                text_empty.setText(DnsSeeker.getInstance().getResources().getString(R.string.empty_recent_blocked));
                text_empty.setVisibility(View.VISIBLE);
            } else {
                text_empty.setVisibility(View.INVISIBLE);
            }
        } else if (isblocked == ALL) {
            myDataset = DnsSeeker.getInstance().getResponse();
            if (myDataset.isEmpty()) {
                text_empty.setText(DnsSeeker.getInstance().getResources().getString(R.string.empty_recent_query));
                text_empty.setVisibility(View.VISIBLE);
            } else {
                text_empty.setVisibility(View.INVISIBLE);
            }
        } else {
            myDataset = DnsSeeker.getInstance().getFailedResponse();
            if (myDataset.isEmpty()) {
                text_empty.setText(DnsSeeker.getInstance().getResources().getString(R.string.empty_recent_failed));
                text_empty.setVisibility(View.VISIBLE);
            } else {
                text_empty.setVisibility(View.INVISIBLE);
            }
        }
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new MyAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);
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
            public TextView domainName;
            public TextView ip;
            public TextView type;
            public TextView timeStamp;
            public CardView inner_card;
            public ImageView icon;
            public View details;
            public TextView whitelist;
            public TextView ip_des;
            public TextView time_des;
            public TextView provider;
            public TextView type_des;

            public MyViewHolder(View v) {
                super(v);
                domainName = v.findViewById(R.id.domainName);
                ip = v.findViewById(R.id.ip);
                type = v.findViewById(R.id.type);
                timeStamp = v.findViewById(R.id.timeStamp);
                inner_card = v.findViewById(R.id.cv_inner);
                icon = v.findViewById(R.id.icon_result);
                details = v.findViewById(R.id.theExpandArea);
                whitelist = v.findViewById(R.id.whitelistBtn);
                provider = v.findViewById(R.id.provider);
                //ip_des = v.findViewById(R.id.ip_des);
                time_des = v.findViewById(R.id.time_des);
                //type_des = v.findViewById(R.id.type_des);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<ResponseRecord> myDataset) {
            Log.d(TAG, "WTF?");
            mDataset = myDataset;
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent,
                                               int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dns_record, parent, false);

            MyViewHolder vh = new MyViewHolder(v);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                    updateReceiver, new IntentFilter("ResponseResult"));
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
            holder.domainName.setText(r.name);
            holder.ip.setText(DnsSeeker.getInstance().getResources().getString(R.string.record_ans, r.IP));
            holder.time_des.setText(DnsSeeker.getInstance().getResources().getString(R.string.record_query_time, r.time));
            holder.type.setText(DnsSeeker.getInstance().getResources().getString(R.string.record_type, r.type));
            holder.timeStamp.setText(r.timeStamp);
            int cardBgText = DnsSeeker.getInstance().getResources().getColor(R.color.cardBgText);
            int cardMaliText = DnsSeeker.getInstance().getResources().getColor(R.color.success);
            int cardFailedText = DnsSeeker.getInstance().getResources().getColor(R.color.white);
            final boolean isExpanded = position == mExpandedPosition;
            if (r.IP.equals("MALICIOUS")) {
                holder.inner_card.setCardBackgroundColor(DnsSeeker.getInstance().getResources().getColor(R.color.theButton));
                holder.ip.setTextColor(cardMaliText);
                holder.type.setTextColor(cardMaliText);
                holder.domainName.setTextColor(cardMaliText);
                holder.icon.setImageResource(R.drawable.ic_block_white_24dp);
                holder.icon.setColorFilter(cardMaliText);
                holder.time_des.setTextColor(cardMaliText);
                holder.timeStamp.setTextColor(cardMaliText);
                holder.whitelist.setBackground(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.rounded_corner_malicious));
                holder.whitelist.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

                if (r.getProviders() != null && r.getProvidersUrl() != null) {
                    String temp = "Provider: " + listToString(r.getProviders(), r.getProvidersUrl());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
                        // we are using this flag to give a consistent behaviour
                        holder.provider.setText(Html.fromHtml(temp, Html.FROM_HTML_MODE_COMPACT));

                    } else {
                        holder.provider.setText(Html.fromHtml(temp));
                    }
                    holder.provider.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.provider.setClickable(true);

                }
                holder.provider.setTextColor(cardMaliText);
                holder.provider.setLinkTextColor(cardMaliText);

                holder.provider.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

                //holder.ip_des.setTextColor(cardBgText);
                //holder.type_des.setTextColor(cardBgText);
            } else if (r.IP.equals("TIMEOUT") || r.IP.equals("No Network Available") || r.IP.equals("SEND_FAIL")) {
                holder.inner_card.setCardBackgroundColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.ip.setTextColor(cardBgText);
                holder.type.setTextColor(cardBgText);
                holder.domainName.setTextColor(cardBgText);
                holder.icon.setImageResource(R.drawable.ic_clear_white_24dp);
                holder.time_des.setTextColor(cardBgText);
                holder.timeStamp.setTextColor(cardBgText);
                holder.whitelist.setBackground(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.rounded_corner_gray));
                holder.whitelist.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                holder.provider.setVisibility(View.GONE);
                //holder.ip_des.setTextColor(cardBgText);
                //holder.type_des.setTextColor(cardBgText);
            } else {
                holder.inner_card.setCardBackgroundColor(DnsSeeker.getInstance().getResources().getColor(R.color.success));
                holder.ip.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.type.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.domainName.setTextColor(Color.BLACK);
                holder.timeStamp.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.time_des.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                holder.icon.setImageResource(R.drawable.ic_check_black_24dp);

                holder.whitelist.setBackground(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.rounded_corner_gray));
                holder.provider.setVisibility(View.GONE);
                holder.whitelist.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

                //holder.ip_des.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
                //holder.type_des.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.cardBg));
            }

            holder.details.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.itemView.setActivated(isExpanded);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mExpandedPosition = isExpanded ? -1 : position;
                    TransitionManager.beginDelayedTransition(mRecyclerView);
                    //notifyDataSetChanged();
                    notifyItemChanged(position);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) DnsSeeker.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Dns Record", r.toString());
                    clipboard.setPrimaryClip(clip);
                    DnsSeeker.popToast("Add to clipboard");
                    return true;
                }
            });
            holder.whitelist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_dialogue_title));
                    builder.setMessage(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_dialogue_content));

                    builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_dialogue_proceed), null);
                    builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_dialogue_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    final AlertDialog mAlertDialog = builder.create();
                    mAlertDialog.show();
                    mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((z -> {
                        DnsSeeker.getStatus().addWhitelistDomain(r.name);
                        DnsSeeker.popToast(r.name + DnsSeeker.getInstance().getResources().getString(R.string.whitelist_is_added));
                        mAlertDialog.dismiss();
                    }));

                }
            });
        }


        public void updateList() {
            List<ResponseRecord> myDataset = new ArrayList<>(DnsSeeker.getInstance().getResponse());
            mDataset.clear();
            mDataset.addAll(myDataset);
            this.notifyDataSetChanged();
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            if (mDataset == null)
                return 0;
            return mDataset.size();
        }

        private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //updateList();
            }
        };

        boolean isValid(String email) {
            String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
            return email.matches(regex);
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
}
