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
 * A dialog that shows contact information of the person who added or accepted an errand. First name, last name, phone number and e-mail address are shown.
 */

public class ContactInfoFragment extends DialogFragment {

    LayoutInflater inflater;
    View view;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.errand_accepter_layout, null);
        final Bundle args = getArguments();

        // Get layout elements
        TextView name = (TextView) view.findViewById(R.id.name);
        TextView phoneNumber = (TextView) view.findViewById(R.id.phoneNumber);
        TextView emailAddress = (TextView) view.findViewById(R.id.email);

        // Set values
        name.setText(args.getString("name"));
        phoneNumber.setText(args.getString("phonenumber"));
        emailAddress.setText(args.getString("email"));

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
