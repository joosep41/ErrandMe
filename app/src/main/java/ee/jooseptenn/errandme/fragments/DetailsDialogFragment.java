package ee.jooseptenn.errandme.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import ee.jooseptenn.errandme.R;

/**
 * A dialog for viewing the details of an errand (title, description, pay and address)
 */

public class DetailsDialogFragment extends DialogFragment {

    LayoutInflater inflater;
    View view;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.errand_details_layout, null);
        final Bundle args = getArguments();

        // Get layout elements
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView description = (TextView) view.findViewById(R.id.description);
        TextView pay = (TextView) view.findViewById(R.id.pay);
        TextView address = (TextView) view.findViewById(R.id.location);
        TextView time = (TextView) view.findViewById(R.id.time);

        // Set values
        title.setText(args.getString("title"));
        description.setText(args.getString("description"));
        pay.setText(args.getString("pay") + " " + args.getString("currency"));
        address.setText(args.getString("location"));

        String timeValue = args.getString("time");
        if (timeValue.equals("none")) {
            time.setText("-");
        }
        else {
            time.setText(timeValue + " minutes");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setPositiveButton(R.string.close_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        return builder.create();
    }



}
