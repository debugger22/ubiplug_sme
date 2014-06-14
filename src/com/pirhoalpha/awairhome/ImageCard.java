package com.pirhoalpha.awairhome;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fima.cardsui.objects.RecyclableCard;

public class ImageCard extends RecyclableCard {

	public ImageCard(String title, int image) {
		super(title, image);
	}

	@Override
	protected int getCardLayoutId() {
		return R.layout.card_picture;
	}

	@Override
	protected void applyTo(View convertView) {
		((TextView) convertView.findViewById(R.id.title)).setText(title);
		((ImageView) convertView.findViewById(R.id.imageView1))
				.setImageResource(image);
	}

}