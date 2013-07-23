package edu.usf.cutr.opentripplanner.android.tasks;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import de.mastacode.http.Http;
import edu.usf.cutr.opentripplanner.android.R;
import edu.usf.cutr.opentripplanner.android.SettingsActivity;
import edu.usf.cutr.opentripplanner.android.listeners.ServerCheckerCompleteListener;
import edu.usf.cutr.opentripplanner.android.model.Server;

public class ServerChecker extends AsyncTask<Server, Long, String> {
	
	private static final String TAG = "OTP";
	private ProgressDialog progressDialog;
	private Context context;

	private ServerCheckerCompleteListener callback = null;
	
	private boolean showMessage;
	private boolean isWorking = false;
	
	/**
     * Constructs a new ServerChecker
     * @param 
     */
	public ServerChecker(Context context, boolean showMessage) {
		this.context = context;
		this.showMessage = showMessage;
    	progressDialog = new ProgressDialog(context);
	}
	
	/**
     * Constructs a new ServerChecker
     * @param 
     */
	public ServerChecker(Context context, ServerCheckerCompleteListener callback, boolean showMessage) {
		this.context = context;
		this.showMessage = showMessage;
		this.callback = callback;
    	progressDialog = new ProgressDialog(context);
	}
	
    @Override
	protected void onPreExecute() {
    	progressDialog = ProgressDialog.show(context,"", context.getString(R.string.server_checker_progress), true);
	}
	
    @Override
	protected String doInBackground(Server... params) {
		Server server = params[0];
		
		if (server == null) {
			Log.w(TAG,
					"Tried to get server info when no server was selected");
			cancel(true);
		}
		
		String message = "Region:  "
				+ server.getRegion();
		message += "\nLanguage:  " + server.getLanguage();
		message += "\nContact:  " + server.getContactName() + " ("
				+ server.getContactEmail() + ")";
		message += "\nURL:  " + server.getBaseURL();

		// TODO - fix server info bounds
		message += "\nBounds: " + server.getBounds();
		message += "\nCurrently reachable: ";

		int status = 0;
		try {
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = context.getResources().getInteger(R.integer.connection_timeout);
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			int timeoutSocket = context.getResources().getInteger(R.integer.socket_timeout);
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			status = Http.get(server.getBaseURL() + "/plan")
					.use(new DefaultHttpClient(httpParameters)).asResponse()
					.getStatusLine().getStatusCode();
		} catch (IOException e) {
			Log.e(TAG, "Unable to reach server: " + e.getMessage());
			message = "Unable to reach server: " + e.getMessage();
			return message;
		}

		if (status == HttpStatus.SC_OK) {
			message += "Yes";
			isWorking = true;
		} else {
			message += "No";
		}

		return message;
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		
		Toast.makeText(context, context.getApplicationContext().getResources().getString(R.string.info_server_error), Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPostExecute(String result) {
		try{
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
		}catch(Exception e){
			Log.e(TAG, "Error in Server Checker PostExecute dismissing dialog: " + e);
		}

		if (showMessage){
			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setTitle("OpenTripPlanner Server Info");
			dialog.setMessage(result);
			dialog.setNeutralButton("OK", null);
			dialog.create().show();
		}
		else{
			if (isWorking){
				Toast.makeText(context, context.getResources().getString(R.string.server_checker_successful), Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(context, context.getResources().getString(R.string.custom_server_error), Toast.LENGTH_SHORT).show();
			}
		}
		
		if (callback != null){
			callback.onServerCheckerComplete(result, isWorking);
		}
	}

}
