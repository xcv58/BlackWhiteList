package org.phone_lab.jouler.blackwhitelist.activities;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import org.phone_lab.jouler.blackwhitelist.R;
import org.phone_lab.jouler.blackwhitelist.utils.Utils;

import java.util.List;

/**
 * Created by xcv58 on 1/23/15.
 */
public class AppAdapter extends ArrayAdapter<App> {
    private final Context context;
    private List<App> list;

    public AppAdapter(Context context, List<App> appList) {
        super(context, R.layout.app_item, appList);
        this.context = context;
        this.list = appList;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.app_item, parent, false);

        TextView textView_label = (TextView) rowView.findViewById(R.id.label);
        TextView textView_name = (TextView) rowView.findViewById(R.id.name);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.logo);
//        CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.check);
        Button blackButton = (Button) rowView.findViewById(R.id.black_button);
        Button whiteButton = (Button) rowView.findViewById(R.id.white_button);

        // set one view
        App app =  list.get(position);
        textView_name.setText(app.getAppName());
//        textView_label.setText(app.getDescription());
        textView_label.setText(app.getPackageName());
        if (app.isSelected()) {
            rowView.setBackgroundColor(0xFFAA66CC);
        }
//        if (client.isSelected()) {
//            textView_label.setVisibility(View.VISIBLE);
//        }
//        if (client.isChoosed()) {
//            checkBox.setChecked(true);
//        }
//
        Drawable icon = app.getIcon();
        imageView.setImageDrawable((icon != null) ? icon : context.getResources().getDrawable( R.drawable.ic_launcher ));

        blackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(Utils.TAG, "Black " + position);
            }
        });
        return rowView;
    }
}
