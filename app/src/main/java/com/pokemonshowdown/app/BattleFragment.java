package com.pokemonshowdown.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pokemonshowdown.data.BattleFieldData;
import com.pokemonshowdown.data.MyApplication;
import com.pokemonshowdown.data.Pokemon;

import java.util.ArrayList;

public class BattleFragment extends android.support.v4.app.Fragment {
    public final static String BTAG = BattleFragment.class.getName();
    public final static String ROOM_ID = "Room Id";

    private String mRoomId;
    private String mPlayer1;
    private String mPlayer2;
    private ArrayList<String> mPlayer1Team;
    private ArrayList<String> mPlayer2Team;

    public static BattleFragment newInstance(String roomId) {
        BattleFragment fragment = new BattleFragment();
        Bundle args = new Bundle();
        args.putString(ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    public BattleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_battle, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            mRoomId = getArguments().getString(ROOM_ID);
        }

        view.findViewById(R.id.battlelog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialogFragment = BattleLogDialog.newInstance(mRoomId);
                dialogFragment.show(getActivity().getSupportFragmentManager(), mRoomId);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        BattleFieldData.AnimationData animationData = BattleFieldData.get(getActivity()).getAnimationInstance(mRoomId);
        if (animationData != null) {
            animationData.setMessageListener(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        BattleFieldData.AnimationData animationData = BattleFieldData.get(getActivity()).getAnimationInstance(mRoomId);
        if (animationData != null) {
            animationData.setMessageListener(true);
        }
    }

    public void processServerMessage(String message) {
        BattleFieldData.AnimationData animationData = BattleFieldData.get(getActivity()).getAnimationInstance(mRoomId);
        String command = (message.indexOf('|') == -1) ? message : message.substring(0, message.indexOf('|'));
        final String messageDetails = message.substring(message.indexOf('|') + 1);
        if (command.startsWith("-")) {
            processMinorAction(command, messageDetails);
            return;
        }

        int separator = messageDetails.indexOf('|');
        int start;
        String remaining;
        String toAppend;
        StringBuilder toAppendBuilder;
        Spannable toAppendSpannable;
        switch (command) {
            case "init":
            case "title":
            case "join":
            case "j":
            case "J":
            case "leave":
            case "l":
            case "L":
            case "chat":
            case "c":
            case "tc":
            case "c:":
                break;
            case "raw":
                makeToast(Html.fromHtml(messageDetails).toString());
                break;
            case "message":
                makeToast(messageDetails);
                break;
            case "gametype":
            case "gen":
                break;
            case "player":
                final String playerType;
                final String playerName;
                final String avatar;
                if (separator == -1) {
                    playerType = messageDetails;
                    playerName = "";
                    avatar = null;
                } else {
                    playerType = messageDetails.substring(0, separator);
                    String playerDetails = messageDetails.substring(separator + 1);
                    separator = playerDetails.indexOf('|');
                    playerName = playerDetails.substring(0, separator);
                    avatar = playerDetails.substring(separator + 1);
                }
                if (playerType.equals("p1")) {
                    animationData.setPlayer1(playerName);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) getView().findViewById(R.id.username)).setText(playerName);
                            if (avatar != null) {
                                int avatarResource = getActivity().getApplicationContext()
                                        .getResources().getIdentifier("avatar_" + avatar, "drawable", getActivity().getApplicationContext().getPackageName());
                                ((ImageView) getView().findViewById(R.id.avatar)).setImageResource(avatarResource);
                            }
                        }
                    });
                    mPlayer1 = playerName;
                } else {
                    animationData.setPlayer2(playerName);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) getView().findViewById(R.id.username_o)).setText(playerName);
                            if (avatar != null) {
                                int avatarResource = getActivity().getApplicationContext()
                                        .getResources().getIdentifier("avatar_" + avatar, "drawable", getActivity().getApplicationContext().getPackageName());
                                ((ImageView) getView().findViewById(R.id.avatar_o)).setImageResource(avatarResource);
                            }
                        }
                    });
                    mPlayer2 = playerName;
                }
                break;
            case "tier":
                break;
            case "rated":
                break;
            case "rule":
                break;
            case "":
                break;
            case "clearpoke":
                mPlayer1Team = new ArrayList<>();
                mPlayer2Team = new ArrayList<>();
                break;
            case "poke":
                playerType = messageDetails.substring(0, separator);
                int comma = messageDetails.indexOf(',');
                final String pokeName = (comma == -1) ? messageDetails.substring(separator + 1) :
                        messageDetails.substring(separator + 1, comma);
                final int iconId;
                if (playerType.equals("p1")) {
                    iconId = mPlayer1Team.size();
                    mPlayer1Team.add(pokeName);
                } else {
                    iconId = mPlayer2Team.size();
                    mPlayer2Team.add(pokeName);
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView icon = (ImageView) getView().findViewById(getIconId(playerType, iconId));
                        icon.setImageResource(Pokemon.getPokemonIconSmall(getActivity(), MyApplication.toId(pokeName), false));
                    }
                });
                break;
            case "teampreview":
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FrameLayout frameLayout = (FrameLayout) getView().findViewById(R.id.battle_interface);
                        frameLayout.removeAllViews();
                        getActivity().getLayoutInflater().inflate(R.layout.fragment_battle_teampreview, frameLayout);
                        for(int i = 0; i < mPlayer1Team.size() ; i++) {
                            ImageView sprites = (ImageView) getView().findViewById(getTeamPreviewSpriteId("p1", i));
                            sprites.setImageResource(Pokemon.getPokemonIcon(getActivity(), MyApplication.toId(mPlayer1Team.get(i)), false));
                        }
                        for(int i = 0; i < mPlayer2Team.size() ; i++) {
                            ImageView sprites = (ImageView) getView().findViewById(getTeamPreviewSpriteId("p2", i));
                            sprites.setImageResource(Pokemon.getPokemonIcon(getActivity(), MyApplication.toId(mPlayer2Team.get(i)), false));
                        }
                    }
                });
                break;
            case "request":
                makeToast(messageDetails);
                break;
            case "inactive":
                final String inactive;
                final String player;
                remaining = messageDetails.substring(0, messageDetails.indexOf(' '));
                if (remaining.equals(mPlayer1)) {
                    player = "p1";
                } else {
                    if (remaining.equals(mPlayer2)) {
                        player = "p2";
                    } else {
                        break;
                    }
                }
                if (messageDetails.contains("has")) {
                    remaining = messageDetails.substring(messageDetails.indexOf("has ") + 4);
                    inactive = remaining.substring(0, remaining.indexOf(' ')) + "s";
                } else {
                    break;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (player.equals("p1")) {
                            ((TextView) getView().findViewById(R.id.inactive)).setText(inactive);
                        } else {
                            ((TextView) getView().findViewById(R.id.inactive_o)).setText(inactive);
                        }
                    }
                });
            case "inactiveoff":
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) getView().findViewById(R.id.inactive)).setText("");
                        ((TextView) getView().findViewById(R.id.inactive_o)).setText("");
                    }
                });
                break;
            case "start":
                /*getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FrameLayout frameLayout = (FrameLayout) getView().findViewById(R.id.battle_interface);
                        frameLayout.removeAllViews();
                        getActivity().getLayoutInflater().inflate(R.layout.fragment_battle_animation, frameLayout);
                    }
                });*/
                break;
            case "move":
                String attacker = messageDetails.substring(5, separator);
                remaining = messageDetails.substring(separator + 1);
                toAppendBuilder = new StringBuilder();
                if (remaining.startsWith("p2")) {
                    toAppendBuilder.append("The opposing's ");
                }
                toAppendBuilder.append(attacker).append(" used ");
                String move = remaining.substring(0, remaining.indexOf('|'));
                toAppendBuilder.append(move).append("!");
                toAppend = toAppendBuilder.toString();
                start = toAppend.indexOf(move);
                toAppendSpannable = new SpannableString(toAppend);
                toAppendSpannable.setSpan(new StyleSpan(Typeface.BOLD), start, start + move.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "switch":
            case "drag":
                toAppendBuilder = new StringBuilder();
                attacker = messageDetails.substring(5, separator);
                remaining = messageDetails.substring(separator + 1);
                separator = remaining.indexOf(',');
                String species = remaining.substring(0, separator);
                attacker = (!attacker.equals(species)) ? attacker + " (" + species + ")" : attacker;
                if (messageDetails.startsWith("p1")) {
                    toAppendBuilder.append("Go! ").append(attacker).append('!');
                } else {
                    toAppendBuilder.append(mPlayer2).append(" sent out ").append(attacker).append("!");
                }
                appendServerMessage(new SpannableStringBuilder(toAppendBuilder));
                break;
            case "detailschange":
                break;
            case "faint":
                attacker = messageDetails.substring(5);
                toAppendBuilder = new StringBuilder();
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker).append(" fainted!");
                appendServerMessage(new SpannableStringBuilder(toAppendBuilder));
                break;
            case "turn":
                toAppend = "TURN " + messageDetails;
                toAppendSpannable = new SpannableString(toAppend.toUpperCase());
                toAppendSpannable.setSpan(new UnderlineSpan(), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                toAppendSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                toAppendSpannable.setSpan(new RelativeSizeSpan(1.25f), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                toAppendSpannable.setSpan(new ForegroundColorSpan(R.color.dark_blue), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "win":
                toAppend = messageDetails + " has won the battle!";
                appendServerMessage(new SpannableString(toAppend));
                break;
            case "cant":
                //todo (cant attack bec frozen/para etc)
                break;
            default:
                appendServerMessage(new SpannableString(message));
        }
    }

    private void processMinorAction(String command, String messageDetails) {
        if (messageDetails.contains("[silent]")) {
            return;
        }

        int separator;
        int start;
        String remaining;
        String toAppend;
        StringBuilder toAppendBuilder = new StringBuilder();
        Spannable toAppendSpannable;
        String move;

        String fromEffect;
        String ofSource;
        int from = messageDetails.indexOf("[from]");
        if (from != -1) {
            remaining = messageDetails.substring(from + 7);
            separator = remaining.indexOf('|');
            fromEffect = (separator == -1) ? remaining : remaining.substring(0, separator);
        }
        int of = messageDetails.indexOf("[of]");
        if (of != -1) {
            remaining = messageDetails.substring(of + 5);
            separator = remaining.indexOf('|');
            ofSource = (separator == -1) ? remaining : remaining.substring(0, separator);
        }

        separator = messageDetails.indexOf('|');
        switch (command) {
            case "message":
                toAppendSpannable = new SpannableString(messageDetails);
                break;
            case "-miss":
                String attacker = messageDetails.substring(5, separator);
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker);
                toAppendBuilder.append(" missed the target");
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;
            case "-fail":
                toAppend = "But it failed!";
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-damage":
                attacker = messageDetails.substring(5, separator);
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker);
                remaining = messageDetails.substring(separator + 1);
                toAppendBuilder.append(" has lost ").append(remaining);
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                    /*
                    separator = remaining.indexOf(" ");
                    String hp = remaining.substring(0, separator);
                    if (hp.equals("0")) {
                        toAppendBuilder.append("has fainted!");
                    }*/
                break;
            case "-heal":
                attacker = messageDetails.substring(5, separator);
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker);
                remaining = messageDetails.substring(separator + 1);
                toAppendBuilder.append(" healed ").append(remaining);
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                    /*
                    separator = remaining.indexOf(" ");
                    String hp = remaining.substring(0, separator);
                    if (hp.equals("0")) {
                        toAppendBuilder.append("has fainted!");
                    }*/
                break;
            case "-status":
                attacker = messageDetails.substring(5, separator);
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker);
                remaining = messageDetails.substring(separator + 1);
                toAppendBuilder.append(" was inflicted with ").append(remaining);
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                    /*
                    separator = remaining.indexOf(" ");
                    String hp = remaining.substring(0, separator);
                    if (hp.equals("0")) {
                        toAppendBuilder.append("has fainted!");
                    }*/
                break;
            case "-curestatus":
                attacker = messageDetails.substring(5, separator);
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker);
                remaining = messageDetails.substring(separator + 1);
                toAppendBuilder.append(" was cured from ").append(remaining);
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                    /*
                    separator = remaining.indexOf(" ");
                    String hp = remaining.substring(0, separator);
                    if (hp.equals("0")) {
                        toAppendBuilder.append("has fainted!");
                    }*/
                break;
            case "-cureteam":
                attacker = messageDetails.substring(5, separator);
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker);
                toAppendBuilder.append(" cured the whole team from bad status");
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                    /*
                    separator = remaining.indexOf(" ");
                    String hp = remaining.substring(0, separator);
                    if (hp.equals("0")) {
                        toAppendBuilder.append("has fainted!");
                    }*/
                break;
            case "-boost":
                attacker = messageDetails.substring(5, separator);
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker);
                remaining = messageDetails.substring(separator + 1);
                toAppendBuilder.append(" has boosted ");
                separator = remaining.indexOf('|');
                String stat = remaining.substring(0, separator);
                String amount = remaining.substring(separator + 1);
                toAppendBuilder.append(stat).append(" by ").append(amount);
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;
            case "-unboost":
                attacker = messageDetails.substring(5, separator);
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker);
                remaining = messageDetails.substring(separator + 1);
                toAppendBuilder.append(" has reduced ");
                separator = remaining.indexOf('|');
                stat = remaining.substring(0, separator);
                amount = remaining.substring(separator + 1);
                toAppendBuilder.append(stat).append(" by ").append(amount);
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;
            case "-weather":
                toAppend = "Weather changed to " + messageDetails;
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-crit":
                toAppendSpannable = new SpannableString("It's a critical hit!");
                break;
            case "-supereffective":
                toAppendSpannable = new SpannableString("It's super effective!");
                break;
            case "-resisted":
                toAppendSpannable = new SpannableString("It's not very effective");
                break;
            case "-immune":
                attacker = messageDetails.substring(5);
                toAppend = attacker + " is immuned";
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-item":
                attacker = messageDetails.substring(5, separator);
                remaining = messageDetails.substring(separator + 1);
                toAppend = attacker + " revealed its " + remaining;
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-enditem":
                attacker = messageDetails.substring(5, separator);
                remaining = messageDetails.substring(separator + 1);
                toAppend = attacker + " has lost its " + remaining;
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-ability":
                attacker = messageDetails.substring(5, separator);
                remaining = messageDetails.substring(separator + 1);
                toAppend = attacker + " revealed its ability " + remaining;
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-endability":
                attacker = messageDetails.substring(5);
                toAppend = attacker + " lost its ability";
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-transform":
                attacker = messageDetails.substring(5, separator);
                remaining = messageDetails.substring(separator + 1);
                toAppend = attacker + " transformed into " + remaining;
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-activate":
                toAppend = messageDetails + " has activated";
                toAppendSpannable = new SpannableString(toAppend);
                break;
            case "-sidestart":
                //reflect, rocks, spikes, light screen, toxic spikes
                // TODO check leech seed maybe?
                if (messageDetails.indexOf("move:") != -1) {
                    move = messageDetails.substring(messageDetails.indexOf("move:") + 5);

                    if (move.indexOf("Stealth Rock") != -1) {
                        toAppendBuilder.append("Pointed stones float in the air around ");
                    } else if (move.indexOf("Toxic Spikes") != -1) {
                        toAppendBuilder.append("Toxic spikes were scattered all around the feet of ");
                    } else if (move.indexOf("Spikes") != -1) {
                        toAppendBuilder.append("Spikes were scattered all around the feet of ");
                    } else if (move.indexOf("Reflect") != -1) {
                        toAppendBuilder.append("A protective veil augments the Defense of ");
                    } else if (move.indexOf("Light Screen") != -1) {
                        toAppendBuilder.append("A protective veil augments the Special Defense of ");
                    } else if (move.indexOf("Sticky Web") != -1) {
                        toAppendBuilder.append("A sticky web spreads out beneath ");
                    }
                }
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("the opposing team!");
                } else {
                    toAppendBuilder.append("your team!");
                }
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;
            case "-sideend":
                // todo
                toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                break;

            case "-hitcount":
                //todo
                toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                break;

            case "-singleturn":
                //todo proctect apparently
                toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                break;

            case "-fieldstart":
                //todo (trick room, maybe more)
                toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                break;

            default:
                toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                break;
        }
        toAppendSpannable.setSpan(new RelativeSizeSpan(0.8f), 0, toAppendSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        appendServerMessage(toAppendSpannable);
    }

    private void appendServerMessage(final Spannable message) {

    }

    private void makeToast(final String message) {
        makeToast(message, Toast.LENGTH_SHORT);
    }

    private void makeToast(final String message, final int duration) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getActivity(), message, duration);
                toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
            }
        });
    }

    /**
     * @param player can be p1 or p2
     */
    private int getTeamPreviewSpriteId(String player, int id) {
        String p = player.substring(0, 2);
        switch (p) {
            case "p1":
                switch (id) {
                    case 0:
                        return R.id.p1a_prev;
                    case 1:
                        return R.id.p1b_prev;
                    case 2:
                        return R.id.p1c_prev;
                    case 3:
                        return R.id.p1d_prev;
                    case 4:
                        return R.id.p1e_prev;
                    case 5:
                        return R.id.p1f_prev;
                    default:
                        return 0;
                }
            case "p2":
                switch (id) {
                    case 0:
                        return R.id.p2a_prev;
                    case 1:
                        return R.id.p2b_prev;
                    case 2:
                        return R.id.p2c_prev;
                    case 3:
                        return R.id.p2d_prev;
                    case 4:
                        return R.id.p2e_prev;
                    case 5:
                        return R.id.p2f_prev;
                    default:
                        return 0;
                }
            default:
                return 0;
        }
    }

    private int getIconId(String player, int id) {
        String p = player.substring(0, 2);
        switch (p) {
            case "p1":
                switch (id) {
                    case 0:
                        return R.id.icon1;
                    case 1:
                        return R.id.icon2;
                    case 2:
                        return R.id.icon3;
                    case 3:
                        return R.id.icon4;
                    case 4:
                        return R.id.icon5;
                    case 5:
                        return R.id.icon6;
                    default:
                        return 0;
                }
            case "p2":
                switch (id) {
                    case 0:
                        return R.id.icon1_o;
                    case 1:
                        return R.id.icon2_o;
                    case 2:
                        return R.id.icon3_o;
                    case 3:
                        return R.id.icon4_o;
                    case 4:
                        return R.id.icon5_o;
                    case 5:
                        return R.id.icon6_o;
                    default:
                        return 0;
                }
            default:
                return 0;
        }
    }

}
