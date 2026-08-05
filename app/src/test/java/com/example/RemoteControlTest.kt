package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.DeviceEntity
import com.example.network.IrCodeMapper
import com.example.network.IrManager
import com.example.ui.remote.BrandType
import com.example.ui.remote.RemoteCommand
import com.example.ui.remote.components.OttAppPillButton
import com.example.ui.remote.components.RealRemoteChassis
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RemoteControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = MainViewModel(context as android.app.Application)
    }

    @Test
    fun `test BrandType parsing from device strings`() {
        assertEquals(BrandType.SAMSUNG, BrandType.fromDeviceType("Samsung Smart TV"))
        assertEquals(BrandType.LG, BrandType.fromDeviceType("LG OLED webOS"))
        assertEquals(BrandType.SONY, BrandType.fromDeviceType("Sony Bravia 4K"))
        assertEquals(BrandType.XIAOMI, BrandType.fromDeviceType("Mi TV 4X"))
        assertEquals(BrandType.ONEPLUS, BrandType.fromDeviceType("OnePlus TV Q1"))
        assertEquals(BrandType.TCL, BrandType.fromDeviceType("iFFALCON / TCL Android TV"))
        assertEquals(BrandType.HISENSE, BrandType.fromDeviceType("Hisense VIDAA TV"))
        assertEquals(BrandType.GENERIC_SMART_TV, BrandType.fromDeviceType(null))
    }

    @Test
    fun `test IrCodeMapper generates non-null hex codes for commands`() {
        val brands = listOf(BrandType.SAMSUNG, BrandType.LG, BrandType.SONY, BrandType.XIAOMI)
        val commands = listOf(RemoteCommand.POWER, RemoteCommand.VOL_UP, RemoteCommand.VOL_DOWN, RemoteCommand.SELECT)

        for (brand in brands) {
            for (cmd in commands) {
                val hexCode = IrCodeMapper.getHexCode(brand, cmd)
                assertNotNull("Hex code for $brand $cmd should not be null", hexCode)
                assertTrue("Hex code should start with 0000", hexCode!!.startsWith("0000"))
            }
        }
    }

    @Test
    fun `test IrManager initialization and transmit gracefully`() {
        val irManager = IrManager(context)
        // IrManager shouldn't crash when calling transmit
        val testHex = IrCodeMapper.getHexCode(BrandType.SAMSUNG, RemoteCommand.POWER)!!
        irManager.transmit(testHex)
    }

    @Test
    fun `test ViewModel save and select device`() = runBlocking {
        val device = DeviceEntity(
            id = 1,
            name = "Living Room TV",
            ipAddress = "192.168.1.100",
            type = "Samsung Smart TV",
            isFavorite = true
        )
        viewModel.saveDevice("Living Room TV", "192.168.1.100", "Samsung Smart TV")
        viewModel.selectDevice(device)

        assertEquals("Living Room TV", viewModel.selectedDevice.value?.name)
        assertEquals("192.168.1.100", viewModel.selectedDevice.value?.ipAddress)

        // Test sending command via ViewModel
        viewModel.sendCommand(RemoteCommand.POWER)
        viewModel.sendCommand(RemoteCommand.VOL_UP)
    }

    @Test
    fun `test RealRemoteChassis UI component renders and handles button taps`() {
        var lastCommandSent: RemoteCommand? = null

        composeTestRule.setContent {
            RealRemoteChassis(
                brandType = BrandType.SAMSUNG,
                hasIrEmitter = true,
                onCommand = { command -> lastCommandSent = command }
            )
        }

        // Test Power button tap
        composeTestRule.onNodeWithTag("btn_samsung_power").performClick()
        assertEquals(RemoteCommand.POWER, lastCommandSent)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Test D-Pad Up tap
        composeTestRule.onNodeWithTag("dpad_up").performClick()
        assertEquals(RemoteCommand.UP, lastCommandSent)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Test D-Pad Select tap
        composeTestRule.onNodeWithTag("dpad_select").performClick()
        assertEquals(RemoteCommand.SELECT, lastCommandSent)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Test Home button tap
        composeTestRule.onNodeWithTag("btn_home").performClick()
        assertEquals(RemoteCommand.HOME, lastCommandSent)
    }

    @Test
    fun `test OttAppPillButton component click`() {
        var appClicked = false
        composeTestRule.setContent {
            OttAppPillButton(
                title = "NETFLIX",
                backgroundColor = androidx.compose.ui.graphics.Color.Red,
                textColor = androidx.compose.ui.graphics.Color.White,
                onClick = { appClicked = true }
            )
        }

        composeTestRule.onNodeWithTag("ott_netflix").performClick()
        assertTrue(appClicked)
    }
}
