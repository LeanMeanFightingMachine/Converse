package uk.lmfm.converse;

import uk.lmfm.converse.R;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.view.Menu;
import android.widget.ImageView;

public class SplashActivity extends Activity {

	/* Graphical objects */
	AnimationDrawable frameAnimation;
	ImageView img;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		loadAnimation();

	}

	protected void onStart() {
		super.onStart();
		if (img != null && frameAnimation != null) {
			frameAnimation.start();
		}
	}

	private void loadAnimation() {
		// Load the ImageView that will host the animation and
		// set its background to our AnimationDrawable XML resource.
		img = (ImageView) this.findViewById(R.id.animationBG);
		img.setBackgroundResource(R.drawable.ripple_anim);

		// Get the background, which has been compiled to an AnimationDrawable
		// object.
		frameAnimation = (AnimationDrawable) img.getBackground();
	}

}
