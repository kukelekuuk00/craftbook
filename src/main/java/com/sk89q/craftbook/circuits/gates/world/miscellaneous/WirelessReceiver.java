// $Id$
/*
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.craftbook.circuits.gates.world.miscellaneous;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.LocalPlayer;
import com.sk89q.craftbook.bukkit.CraftBookPlugin;
import com.sk89q.craftbook.circuits.ic.AbstractICFactory;
import com.sk89q.craftbook.circuits.ic.AbstractSelfTriggeredIC;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.ConfigurableIC;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.circuits.ic.ICMechanic;
import com.sk89q.craftbook.circuits.ic.ICVerificationException;
import com.sk89q.craftbook.util.UUIDFetcher;
import com.sk89q.util.yaml.YAMLProcessor;

public class WirelessReceiver extends AbstractSelfTriggeredIC {

    private String band;

    public WirelessReceiver(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    @Override
    public void load() {

        band = getSign().getLine(2);
        if (!getLine(3).trim().isEmpty()) {
            if(CraftBookPlugin.inst().getConfiguration().convertNamesToCBID) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(band);
                if(player.hasPlayedBefore()) {
                    UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(band));
                    try {
                        UUID uuid = fetcher.call().get(band);
                        band = CraftBookPlugin.inst().getUUIDMappings().getCBID(uuid);
                        getSign().setLine(3, band);
                        getSign().update(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            band = band + getSign().getLine(3);
        }
    }

    @Override
    public String getTitle() {

        return "Wireless Receiver";
    }

    @Override
    public String getSignTitle() {

        return "RECEIVER";
    }

    @Override
    public void trigger(ChipState chip) {

        if (chip.getInput(0)) {

            chip.setOutput(0, getOutput());
        }
    }

    public boolean getOutput() {

        Boolean val = WirelessTransmitter.getValue(band);

        if (val == null) {
            return false;
        }

        return val;
    }

    @Override
    public void think(ChipState chip) {

        chip.setOutput(0, getOutput());
    }

    public static class Factory extends AbstractICFactory implements ConfigurableIC {

        public boolean requirename;

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new WirelessReceiver(getServer(), sign, this);
        }

        @Override
        public String[] getLongDescription() {

            return new String[]{
                    "The '''MC1111''' receives the state in a particular ''band'' or ''network'' when the clock input goes from low to high.",
                    "The corresponding transmitter is the [[../MC1110/]] IC.",
                    "",
                    "If there are multiple transmitters for the same band, the last one to transmit to a particular band will have its state apply until the next transmission."
            };
        }

        @Override
        public String getShortDescription() {

            return "Recieves signal from wireless transmitter.";
        }

        @Override
        public String[] getPinDescription(ChipState state) {

            return new String[] {
                    "Trigger IC",//Inputs
                    "State of Wireless Band",//Outputs
            };
        }

        @Override
        public String[] getLineHelp() {

            return new String[] {"Wireless Band", "Player's CBID (Automatic)"};
        }

        @Override
        public void checkPlayer(ChangedSign sign, LocalPlayer player) throws ICVerificationException {

            if (requirename && (sign.getLine(3).isEmpty() || !ICMechanic.hasRestrictedPermissions(player, this, "MC1111"))) sign.setLine(3, player.getCraftBookId());
            else if (!sign.getLine(3).isEmpty() && !ICMechanic.hasRestrictedPermissions(player, this, "MC1111")) sign.setLine(3, player.getCraftBookId());
            sign.update(false);
        }

        @Override
        public void addConfiguration(YAMLProcessor config, String path) {

            config.setComment(path + "per-player", "Require a name to be entered on the sign. This allows for 'per-player' wireless bands. This is done automatically.");
            requirename = config.getBoolean(path + "per-player", false);
        }
    }

    @Override
    public boolean isActive () {
        return true;
    }
}