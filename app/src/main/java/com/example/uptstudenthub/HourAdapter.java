package com.example.uptstudenthub;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Collections;
import java.util.List;

/** Adapter das horas de um dia; destaca as horas com chuva acima do limiar (V1). */
public final class HourAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private List<HourlyForecast> items = Collections.emptyList();
    private int threshold;
    private boolean celsius = true;

    /** @param context contexto usado para inflar linhas e obter textos */
    public HourAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * Substitui os dados mostrados.
     *
     * @param items     horas do dia
     * @param threshold limiar de chuva em %
     * @param celsius   true se a unidade é Celsius
     */
    public void setData(List<HourlyForecast> items, int threshold, boolean celsius) {
        this.items = items;
        this.threshold = threshold;
        this.celsius = celsius;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public HourlyForecast getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_hour, parent, false);
            h = new ViewHolder(convertView);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }
        HourlyForecast f = items.get(position);
        h.icon.setImageResource(WeatherCodes.iconFor(f.weatherCode));
        h.hour.setText(Format.hour(f.time));
        h.temp.setText(Format.temperature(context, f.temperature, celsius));
        h.rain.setText(Format.rain(context, f.precipProbability));
        boolean high = f.precipProbability != null && f.precipProbability >= threshold;
        convertView.setBackgroundColor(high
                ? ContextCompat.getColor(context, R.color.ex_rain_highlight)
                : Color.TRANSPARENT);
        return convertView;
    }

    /** Guarda as Views de uma linha. */
    private static final class ViewHolder {
        final ImageView icon;
        final TextView hour;
        final TextView temp;
        final TextView rain;

        ViewHolder(View root) {
            icon = root.findViewById(R.id.ivHourIcon);
            hour = root.findViewById(R.id.tvHour);
            temp = root.findViewById(R.id.tvHourTemp);
            rain = root.findViewById(R.id.tvHourRain);
        }
    }
}
