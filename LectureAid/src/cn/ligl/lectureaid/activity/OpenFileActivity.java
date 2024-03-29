package cn.ligl.lectureaid.activity;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.ligl.lectureaid.R;
import cx.hell.android.pagesview.PagesView;
import cx.hell.android.pdfview.Actions;
import cx.hell.android.pdfview.Bookmark;
import cx.hell.android.pdfview.BookmarkEntry;
import cx.hell.android.pdfview.Options;
import cx.hell.android.pdfview.PDF;
import cx.hell.android.pdfview.PDFPagesProvider;

/**
 * Document display activity.
 */
public class OpenFileActivity extends Activity implements SensorEventListener {

	private final static String TAG = "cx.hell.android.pdfview";

	private final static int[] zoomAnimations = { R.anim.zoom_disappear,
			R.anim.zoom_almost_disappear, R.anim.zoom };

	private final static int[] pageNumberAnimations = { R.anim.page_disappear,
			R.anim.page_almost_disappear, R.anim.page, R.anim.page_show_always };

	private PDF pdf = null;
	private PagesView pagesView = null;
	private PDFPagesProvider pdfPagesProvider = null;
	private Actions actions = null;

	private Handler zoomHandler = null;
	private Handler pageHandler = null;
	private Runnable zoomRunnable = null;
	private Runnable pageRunnable = null;

	private MenuItem aboutMenuItem = null;
	private MenuItem gotoPageMenuItem = null;
	private MenuItem rotateLeftMenuItem = null;
	private MenuItem rotateRightMenuItem = null;
	private MenuItem chooseFileMenuItem = null;
	private MenuItem optionsMenuItem = null;

	private EditText pageNumberInputField = null;

	private RelativeLayout activityLayout = null;
	private boolean eink = false;

	// currently opened file path
	private String filePath = "/";

	// zoom buttons, layout and fade animation
	private ImageButton zoomDownButton;
	private ImageButton zoomWidthButton;
	private ImageButton zoomUpButton;
	private Animation zoomAnim;
	private LinearLayout zoomLayout;

	// page number display
	private TextView pageNumberTextView;
	private Animation pageNumberAnim;

	private int box = 2;

	public boolean showZoomOnScroll = false;

	private int fadeStartOffset = 7000;

	private int colorMode = Options.COLOR_MODE_NORMAL;

	private SensorManager sensorManager;
	private static final int ZOOM_COLOR_NORMAL = 0;
	private static final int ZOOM_COLOR_RED = 1;
	private static final int ZOOM_COLOR_GREEN = 2;
	private static final int[] zoomUpId = { R.drawable.btn_zoom_up,
			R.drawable.red_btn_zoom_up, R.drawable.green_btn_zoom_up };
	private static final int[] zoomDownId = { R.drawable.btn_zoom_down,
			R.drawable.red_btn_zoom_down, R.drawable.green_btn_zoom_down };
	private static final int[] zoomWidthId = { R.drawable.btn_zoom_width,
			R.drawable.red_btn_zoom_width, R.drawable.green_btn_zoom_width };
	private float[] gravity = { 0f, -9.81f, 0f };

	private int prevOrientation;

	private boolean history = true;

	/**
	 * Called when the activity is first created. TODO: initialize dialog fast,
	 * then move file loading to other thread TODO: add progress bar for file
	 * load TODO: add progress icon for file rendering
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Options.setOrientation(this);
		SharedPreferences options = PreferenceManager
				.getDefaultSharedPreferences(this);

		this.box = Integer.parseInt(options.getString(Options.PREF_BOX, "2"));
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Get display metrics
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		// use a relative layout to stack the views
		activityLayout = new RelativeLayout(this);

		// the PDF view
		this.pagesView = new PagesView(this);
		activityLayout.addView(pagesView);
		startPDF(options);
		if (!this.pdf.isValid()) {
			finish();
		}

		this.pageNumberTextView = new TextView(this);
		this.pageNumberTextView.setTextSize(8f * metrics.density);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		activityLayout.addView(this.pageNumberTextView, lp);

		// display this
		this.setContentView(activityLayout);

		// go to last viewed page
		// gotoLastPage();

		// send keyboard events to this view
		pagesView.setFocusable(true);
		pagesView.setFocusableInTouchMode(true);

		this.zoomHandler = new Handler();
		this.pageHandler = new Handler();
		this.zoomRunnable = new Runnable() {
			public void run() {
				fadeZoom();
			}
		};
		this.pageRunnable = new Runnable() {
			public void run() {
				fadePage();
			}
		};
	}

	/**
	 * Save the current page before exiting
	 */
	@Override
	protected void onPause() {
		super.onPause();

		saveLastPage();

		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
			sensorManager = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		sensorManager = null;

		if (Options.setOrientation(this)) {
			sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
				gravity[0] = 0f;
				gravity[1] = -9.81f;
				gravity[2] = 0f;
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_NORMAL);
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
				this.prevOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
			} else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		}

		SharedPreferences options = PreferenceManager
				.getDefaultSharedPreferences(this);

		history = options.getBoolean(Options.PREF_HISTORY, true);
		boolean eink = options.getBoolean(Options.PREF_EINK, false);
		this.pagesView.setEink(eink);
		if (eink)
			this.setTheme(android.R.style.Theme_Light);
		this.pagesView.setNook2(options.getBoolean(Options.PREF_NOOK2, false));

		if (options.getBoolean(Options.PREF_KEEP_ON, false))
			this.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			this.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		actions = new Actions(options);
		this.pagesView.setActions(actions);

		setZoomLayout(options);

		this.pagesView.setZoomLayout(zoomLayout);

		this.showZoomOnScroll = options.getBoolean(
				Options.PREF_SHOW_ZOOM_ON_SCROLL, false);
		this.pagesView.setSideMargins(Integer.parseInt(options.getString(
				Options.PREF_SIDE_MARGINS, "0")));
		this.pagesView.setTopMargin(Integer.parseInt(options.getString(
				Options.PREF_TOP_MARGIN, "0")));

		this.pagesView.setDoubleTap(Integer.parseInt(options.getString(
				Options.PREF_DOUBLE_TAP, "" + Options.DOUBLE_TAP_ZOOM_IN_OUT)));

		int newBox = Integer.parseInt(options.getString(Options.PREF_BOX, "2"));
		if (this.box != newBox) {
			saveLastPage();
			this.box = newBox;
			startPDF(options);
			this.pagesView.goToBookmark();
		}

		this.colorMode = Options.getColorMode(options);
		this.eink = options.getBoolean(Options.PREF_EINK, false);
		this.pageNumberTextView.setBackgroundColor(Options
				.getBackColor(colorMode));
		this.pageNumberTextView.setTextColor(Options.getForeColor(colorMode));
		this.pdfPagesProvider.setGray(Options.isGray(this.colorMode));
		this.pdfPagesProvider.setExtraCache(1024 * 1024 * Options
				.getIntFromString(options, Options.PREF_EXTRA_CACHE, 0));
		this.pdfPagesProvider.setOmitImages(options.getBoolean(
				Options.PREF_OMIT_IMAGES, false));
		this.pagesView.setColorMode(this.colorMode);

		this.pdfPagesProvider.setRenderAhead(options.getBoolean(
				Options.PREF_RENDER_AHEAD, true));
		this.pagesView.setVerticalScrollLock(options.getBoolean(
				Options.PREF_VERTICAL_SCROLL_LOCK, false));
		this.pagesView.invalidate();
		int zoomAnimNumber = Integer.parseInt(options.getString(
				Options.PREF_ZOOM_ANIMATION, "2"));

		if (zoomAnimNumber == Options.ZOOM_BUTTONS_DISABLED)
			zoomAnim = null;
		else
			zoomAnim = AnimationUtils.loadAnimation(this,
					zoomAnimations[zoomAnimNumber]);
		int pageNumberAnimNumber = Integer.parseInt(options.getString(
				Options.PREF_PAGE_ANIMATION, "3"));

		if (pageNumberAnimNumber == Options.PAGE_NUMBER_DISABLED)
			pageNumberAnim = null;
		else
			pageNumberAnim = AnimationUtils.loadAnimation(this,
					pageNumberAnimations[pageNumberAnimNumber]);

		fadeStartOffset = 1000 * Integer.parseInt(options.getString(
				Options.PREF_FADE_SPEED, "7"));

		if (options.getBoolean(Options.PREF_FULLSCREEN, false))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		else
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.pageNumberTextView
				.setVisibility(pageNumberAnim == null ? View.GONE
						: View.VISIBLE);
		this.zoomLayout.setVisibility(zoomAnim == null ? View.GONE
				: View.VISIBLE);

		showAnimated(true);
	}

	/**
	 * Set handlers on zoom level buttons
	 */
	private void setZoomButtonHandlers() {
		this.zoomDownButton
				.setOnLongClickListener(new View.OnLongClickListener() {
					public boolean onLongClick(View v) {
						pagesView.doAction(actions
								.getAction(Actions.LONG_ZOOM_IN));
						return true;
					}
				});
		this.zoomDownButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pagesView.doAction(actions.getAction(Actions.ZOOM_IN));
			}
		});
		this.zoomWidthButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pagesView.zoomWidth();
			}
		});
		this.zoomWidthButton
				.setOnLongClickListener(new View.OnLongClickListener() {
					public boolean onLongClick(View v) {
						pagesView.zoomFit();
						return true;
					}
				});
		this.zoomUpButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pagesView.doAction(actions.getAction(Actions.ZOOM_OUT));
			}
		});
		this.zoomUpButton
				.setOnLongClickListener(new View.OnLongClickListener() {
					public boolean onLongClick(View v) {
						pagesView.doAction(actions
								.getAction(Actions.LONG_ZOOM_OUT));
						return true;
					}
				});
	}

	private void startPDF(SharedPreferences options) {
		this.pdf = this.getPDF();
		if (!this.pdf.isValid()) {
			Log.v(TAG, "Invalid PDF");
			if (this.pdf.isInvalidPassword()) {
				Toast.makeText(this, "This file needs a password", 4000).show();
			} else {
				Toast.makeText(this, "Invalid PDF file", 4000).show();
			}
			return;
		}
		this.colorMode = Options.getColorMode(options);
		this.pdfPagesProvider = new PDFPagesProvider(this, pdf,
				Options.isGray(this.colorMode), options.getBoolean(
						Options.PREF_OMIT_IMAGES, false), options.getBoolean(
						Options.PREF_RENDER_AHEAD, true));
		pagesView.setPagesProvider(pdfPagesProvider);
		Bookmark b = new Bookmark(this.getApplicationContext()).open();
		pagesView.setStartBookmark(b, filePath);
		b.close();
	}

	/**
	 * Return PDF instance wrapping file referenced by Intent. Currently reads
	 * all bytes to memory, in future local files should be passed to native
	 * code and remote ones should be downloaded to local tmp dir.
	 * 
	 * @return PDF instance
	 */
	private PDF getPDF() {
		final Intent intent = getIntent();
		Uri uri = intent.getData();
		filePath = uri.getPath();
		if (uri.getScheme().equals("file")) {
			if (history) {
				// TODO don't need
				// Recent recent = new Recent(this);
				// recent.add(0, filePath);
				// recent.commit();
			}
			return new PDF(new File(filePath), this.box);
		} else if (uri.getScheme().equals("content")) {
			ContentResolver cr = this.getContentResolver();
			FileDescriptor fileDescriptor;
			try {
				fileDescriptor = cr.openFileDescriptor(uri, "r")
						.getFileDescriptor();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e); // TODO: handle errors
			}
			return new PDF(fileDescriptor, this.box);
		} else {
			throw new RuntimeException("don't know how to get filename from "
					+ uri);
		}
	}

	/**
	 * Handle menu.
	 * 
	 * @param menuItem
	 *            selected menu item
	 * @return true if menu item was handled
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		if (menuItem == this.aboutMenuItem) {
			// Intent intent = new Intent();
			// intent.setClass(this, AboutPDFViewActivity.class);
			// this.startActivity(intent);
			return true;
		} else if (menuItem == this.gotoPageMenuItem) {
			this.showGotoPageDialog();
		} else if (menuItem == this.rotateLeftMenuItem) {
			this.pagesView.rotate(-1);
		} else if (menuItem == this.rotateRightMenuItem) {
			this.pagesView.rotate(1);
		} else if (menuItem == this.chooseFileMenuItem) {
			// startActivity(new Intent(this, ChooseFileActivity.class));
		} else if (menuItem == this.optionsMenuItem) {
			// startActivity(new Intent(this, Options.class));
		}
		return false;
	}

	private void setOrientation(int orientation) {
		if (orientation != this.prevOrientation) {
			setRequestedOrientation(orientation);
			this.prevOrientation = orientation;
		}
	}

	/**
	 * Intercept touch events to handle the zoom buttons animation
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		int action = event.getAction();
		if (action == MotionEvent.ACTION_UP
				|| action == MotionEvent.ACTION_DOWN) {
			showPageNumber(true);
			if (showZoomOnScroll) {
				showZoom();
			}
		}
		return super.dispatchTouchEvent(event);
	};

	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		if (action == KeyEvent.ACTION_UP || action == KeyEvent.ACTION_DOWN) {
			if (!eink)
				showAnimated(false);
		}
		return super.dispatchKeyEvent(event);
	};

	public void showZoom() {
		if (zoomAnim == null) {
			zoomLayout.setVisibility(View.GONE);
			return;
		}

		zoomLayout.clearAnimation();
		zoomLayout.setVisibility(View.VISIBLE);
		zoomHandler.removeCallbacks(zoomRunnable);
		zoomHandler.postDelayed(zoomRunnable, fadeStartOffset);
	}

	private void fadeZoom() {
		if (eink || zoomAnim == null) {
			zoomLayout.setVisibility(View.GONE);
		} else {
			zoomAnim.setStartOffset(0);
			zoomAnim.setFillAfter(true);
			zoomLayout.startAnimation(zoomAnim);
		}
	}

	public void showPageNumber(boolean force) {
		if (pageNumberAnim == null) {
			pageNumberTextView.setVisibility(View.GONE);
			return;
		}

		pageNumberTextView.setVisibility(View.VISIBLE);
		String newText = "" + (this.pagesView.getCurrentPage() + 1) + "/"
				+ this.pdfPagesProvider.getPageCount();

		if (!force && newText.equals(pageNumberTextView.getText()))
			return;

		pageNumberTextView.setText(newText);
		pageNumberTextView.clearAnimation();

		pageHandler.removeCallbacks(pageRunnable);
		pageHandler.postDelayed(pageRunnable, fadeStartOffset);
	}

	private void fadePage() {
		if (eink || pageNumberAnim == null) {
			pageNumberTextView.setVisibility(View.GONE);
		} else {
			pageNumberAnim.setStartOffset(0);
			pageNumberAnim.setFillAfter(true);
			pageNumberTextView.startAnimation(pageNumberAnim);
		}
	}

	/**
	 * Show zoom buttons and page number
	 */
	public void showAnimated(boolean alsoZoom) {
		if (alsoZoom)
			showZoom();
		showPageNumber(true);
	}

	/**
	 * Show error message to user.
	 * 
	 * @param message
	 *            message to show
	 */
	private void errorMessage(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog = builder.setMessage(message).setTitle("Error")
				.create();
		dialog.show();
	}

	/**
	 * Called from menu when user want to go to specific page.
	 */
	private void showGotoPageDialog() {
		final Dialog d = new Dialog(this);
		d.setTitle(R.string.goto_page_dialog_title);
		LinearLayout contents = new LinearLayout(this);
		contents.setOrientation(LinearLayout.VERTICAL);
		TextView label = new TextView(this);
		final int pagecount = this.pdfPagesProvider.getPageCount();
		label.setText("Page number from " + 1 + " to " + pagecount);
		this.pageNumberInputField = new EditText(this);
		this.pageNumberInputField.setInputType(InputType.TYPE_CLASS_NUMBER);
		this.pageNumberInputField.setText(""
				+ (this.pagesView.getCurrentPage() + 1));
		Button goButton = new Button(this);
		goButton.setText(R.string.goto_page_go_button);
		goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				int pageNumber = -1;
				try {
					pageNumber = Integer
							.parseInt(OpenFileActivity.this.pageNumberInputField
									.getText().toString()) - 1;
				} catch (NumberFormatException e) {
					/* ignore */
				}
				d.dismiss();
				if (pageNumber >= 0 && pageNumber < pagecount) {
					OpenFileActivity.this.gotoPage(pageNumber);

				} else {
					OpenFileActivity.this.errorMessage("Invalid page number");
				}
			}
		});
		Button page1Button = new Button(this);
		page1Button.setText(getResources().getString(R.string.page) + " 1");
		page1Button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
				OpenFileActivity.this.gotoPage(0);
			}
		});
		Button lastPageButton = new Button(this);
		lastPageButton.setText(getResources().getString(R.string.page) + " "
				+ pagecount);
		lastPageButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
				OpenFileActivity.this.gotoPage(pagecount - 1);
			}
		});
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = 5;
		params.rightMargin = 5;
		params.bottomMargin = 2;
		params.topMargin = 2;
		contents.addView(label, params);
		contents.addView(pageNumberInputField, params);
		contents.addView(goButton, params);
		contents.addView(page1Button, params);
		contents.addView(lastPageButton, params);
		d.setContentView(contents);
		d.show();
	}

	private void gotoPage(int page) {
		Log.i(TAG, "rewind to page " + page);
		if (this.pagesView != null) {
			this.pagesView.scrollToPage(page);
			showAnimated(true);
		}
	}

	/**
	 * Save the last page in the bookmarks
	 */
	private void saveLastPage() {
		BookmarkEntry entry = this.pagesView.toBookmarkEntry();
		Bookmark b = new Bookmark(this.getApplicationContext()).open();
		b.setLast(filePath, entry);
		b.close();
		Log.i(TAG, "last page saved for " + filePath);
	}

	/**
	 * 
	 * Create options menu, called by Android system.
	 * 
	 * @param menu
	 *            menu to populate
	 * @return true meaning that menu was populated
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		this.gotoPageMenuItem = menu.add(R.string.goto_page);
		this.rotateRightMenuItem = menu.add(R.string.rotate_page_left);
		this.rotateLeftMenuItem = menu.add(R.string.rotate_page_right);
		// this.chooseFileMenuItem = menu.add(R.string.choose_file);
		// this.optionsMenuItem = menu.add(R.string.options);
		/*
		 * The following appear on the second page. The find item can safely be
		 * kept there since it can also be accessed from the search key on most
		 * devices.
		 */
		// this.aboutMenuItem = menu.add(R.string.about);
		return true;
	}

	/**
	 * Prepare menu contents. Hide or show "Clear find results" menu item
	 * depending on whether we're in find mode.
	 * 
	 * @param menu
	 *            menu that should be prepared
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.i(TAG, "onConfigurationChanged(" + newConfig + ")");
	}

	private void setZoomLayout(SharedPreferences options) {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		int colorMode = Options.getColorMode(options);
		int mode = ZOOM_COLOR_NORMAL;

		if (colorMode == Options.COLOR_MODE_GREEN_ON_BLACK) {
			mode = ZOOM_COLOR_GREEN;
		} else if (colorMode == Options.COLOR_MODE_RED_ON_BLACK) {
			mode = ZOOM_COLOR_RED;
		}

		// the zoom buttons
		if (zoomLayout != null) {
			activityLayout.removeView(zoomLayout);
		}

		zoomLayout = new LinearLayout(this);
		zoomLayout.setOrientation(LinearLayout.HORIZONTAL);
		zoomDownButton = new ImageButton(this);
		zoomDownButton.setImageDrawable(getResources().getDrawable(
				zoomDownId[mode]));
		zoomDownButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomDownButton, (int) (80 * metrics.density),
				(int) (50 * metrics.density)); // TODO: remove hardcoded values
		zoomWidthButton = new ImageButton(this);
		zoomWidthButton.setImageDrawable(getResources().getDrawable(
				zoomWidthId[mode]));
		zoomWidthButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomWidthButton, (int) (58 * metrics.density),
				(int) (50 * metrics.density));
		zoomUpButton = new ImageButton(this);
		zoomUpButton.setImageDrawable(getResources()
				.getDrawable(zoomUpId[mode]));
		zoomUpButton.setBackgroundColor(Color.TRANSPARENT);
		zoomLayout.addView(zoomUpButton, (int) (80 * metrics.density),
				(int) (50 * metrics.density));
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		setZoomButtonHandlers();
		activityLayout.addView(zoomLayout, lp);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0];
		gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1];
		gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2];

		float sq0 = gravity[0] * gravity[0];
		float sq1 = gravity[1] * gravity[1];
		float sq2 = gravity[2] * gravity[2];

		if (sq1 > .85 * (sq0 + sq2)) {
			if (gravity[1] > 4)
				setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			else if (gravity[1] < -4
					&& Integer.parseInt(Build.VERSION.SDK) >= 9)
				setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
		} else if (sq0 > .85 * (sq1 + sq2)) {
			if (gravity[0] > 4)
				setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			else if (gravity[0] < -4
					&& Integer.parseInt(Build.VERSION.SDK) >= 9)
				setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		}
	}
}
