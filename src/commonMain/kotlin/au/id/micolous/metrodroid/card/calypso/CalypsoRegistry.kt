/*
 * CalypsoRegistry.kt
 *
 * Copyright 2018 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.card.calypso

import au.id.micolous.metrodroid.transit.intercode.IntercodeTransitData
import au.id.micolous.metrodroid.transit.lisboaviva.LisboaVivaTransitData
import au.id.micolous.metrodroid.transit.mobib.MobibTransitData
import au.id.micolous.metrodroid.transit.opus.OpusTransitData
import au.id.micolous.metrodroid.transit.pisa.PisaTransitData
import au.id.micolous.metrodroid.transit.ravkav.RavKavTransitData
import au.id.micolous.metrodroid.transit.venezia.VeneziaTransitData

object CalypsoRegistry {
    val allFactories: List<CalypsoCardTransitFactory> = listOf(
            RavKavTransitData.FACTORY,
            OpusTransitData.FACTORY,
            MobibTransitData.FACTORY,
            IntercodeTransitData.FACTORY,
            LisboaVivaTransitData.FACTORY,
            VeneziaTransitData.FACTORY,
            PisaTransitData.FACTORY)
}