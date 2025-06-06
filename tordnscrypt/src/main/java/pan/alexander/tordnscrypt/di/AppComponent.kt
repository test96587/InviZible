/*
    This file is part of InviZible Pro.

    InviZible Pro is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    InviZible Pro is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2025 by Garmatin Oleksandr invizible.soft@gmail.com
 */

package pan.alexander.tordnscrypt.di

import android.content.Context
import androidx.annotation.Keep
import dagger.BindsInstance
import dagger.Component
import pan.alexander.tordnscrypt.BootCompleteReceiver
import pan.alexander.tordnscrypt.MainActivity
import pan.alexander.tordnscrypt.TopFragment
import pan.alexander.tordnscrypt.about.AboutActivity
import pan.alexander.tordnscrypt.backup.BackupFragment
import pan.alexander.tordnscrypt.backup.BackupHelper
import pan.alexander.tordnscrypt.di.arp.ArpSubcomponent
import pan.alexander.tordnscrypt.di.modulesservice.ModulesServiceSubcomponent
import pan.alexander.tordnscrypt.di.tiles.TilesSubcomponent
import pan.alexander.tordnscrypt.dialogs.*
import pan.alexander.tordnscrypt.dialogs.progressDialogs.PleaseWaitDialogBridgesRequest

import pan.alexander.tordnscrypt.dnscrypt_fragment.DNSCryptFragmentReceiver
import pan.alexander.tordnscrypt.domain.log_reader.dnscrypt.DNSCryptLogParser
import pan.alexander.tordnscrypt.domain.log_reader.tor.TorLogParser
import pan.alexander.tordnscrypt.domain.preferences.PreferenceRepository
import pan.alexander.tordnscrypt.help.HelpActivity
import pan.alexander.tordnscrypt.help.HelpActivityReceiver
import pan.alexander.tordnscrypt.installer.Installer
import pan.alexander.tordnscrypt.iptables.IptablesReceiver
import pan.alexander.tordnscrypt.iptables.Tethering
import pan.alexander.tordnscrypt.itpd_fragment.ITPDFragmentReceiver
import pan.alexander.tordnscrypt.main_fragment.MainFragment
import pan.alexander.tordnscrypt.modules.*
import pan.alexander.tordnscrypt.proxy.ProxyFragment
import pan.alexander.tordnscrypt.settings.*
import pan.alexander.tordnscrypt.settings.dnscrypt_relays.PreferencesDNSCryptRelays
import pan.alexander.tordnscrypt.settings.dnscrypt_servers.PreferencesDNSCryptServers
import pan.alexander.tordnscrypt.settings.dnscrypt_settings.RulesEraser
import pan.alexander.tordnscrypt.settings.dnscrypt_settings.PreferencesDNSFragment
import pan.alexander.tordnscrypt.settings.firewall.FirewallFragment
import pan.alexander.tordnscrypt.settings.itpd_settings.PreferencesITPDFragment
import pan.alexander.tordnscrypt.settings.dnscrypt_rules.DnsRulesFragment
import pan.alexander.tordnscrypt.settings.dnscrypt_rules.local.UpdateLocalDnsRulesWorker
import pan.alexander.tordnscrypt.settings.tor_apps.UnlockTorAppsFragment
import pan.alexander.tordnscrypt.settings.tor_bridges.BridgeAdapter
import pan.alexander.tordnscrypt.settings.tor_bridges.PreferencesTorBridges
import pan.alexander.tordnscrypt.settings.tor_ips.UnlockTorIpsFragment
import pan.alexander.tordnscrypt.settings.tor_preferences.PreferencesTorFragment
import pan.alexander.tordnscrypt.tor_fragment.TorFragmentReceiver
import pan.alexander.tordnscrypt.update.DownloadTask
import pan.alexander.tordnscrypt.update.UpdateCheck
import pan.alexander.tordnscrypt.update.UpdateService
import pan.alexander.tordnscrypt.utils.apps.InstalledApplicationsManager
import pan.alexander.tordnscrypt.utils.executors.CachedExecutor
import pan.alexander.tordnscrypt.utils.executors.CoroutineExecutor
import pan.alexander.tordnscrypt.utils.filemanager.FileManager
import pan.alexander.tordnscrypt.utils.integrity.Verifier
import pan.alexander.tordnscrypt.utils.root.RootExecService
import pan.alexander.tordnscrypt.utils.web.TorRefreshIPsWork
import pan.alexander.tordnscrypt.settings.dnscrypt_rules.remote.UpdateRemoteDnsRulesWorker
import pan.alexander.tordnscrypt.settings.dnscrypt_rules.existing.RemixExistingDnsRulesWorker
import pan.alexander.tordnscrypt.settings.itpd_settings.ITPDSubscriptionsFragment
import pan.alexander.tordnscrypt.vpn.service.ServiceVPNHandler
import javax.inject.Singleton

@Singleton
@Component(
    modules = [SharedPreferencesModule::class, RepositoryModule::class,
        DataSourcesModule::class, HelpersModule::class, CoroutinesModule::class,
        HandlerModule::class, InteractorsModule::class, ViewModelModule::class,
        AppSubcomponentModule::class]
)
@Keep
interface AppComponent {
    fun tilesSubcomponent(): TilesSubcomponent.Factory
    fun arpSubcomponent(): ArpSubcomponent.Factory
    fun modulesServiceSubcomponent(): ModulesServiceSubcomponent.Factory

    fun getPathVars(): dagger.Lazy<PathVars>
    fun getPreferenceRepository(): dagger.Lazy<PreferenceRepository>
    fun getCachedExecutor(): CachedExecutor
    fun getCoroutineExecutor(): CoroutineExecutor

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun appContext(context: Context): Builder
        fun build(): AppComponent
    }

    fun inject(activity: MainActivity)
    fun inject(activity: SettingsActivity)
    fun inject(activity: HelpActivity)
    fun inject(activity: AboutActivity)
    fun inject(fragment: TopFragment)
    fun inject(fragment: MainFragment)
    fun inject(fragment: UnlockTorAppsFragment)
    fun inject(fragment: PreferencesTorBridges)
    fun inject(fragment: UnlockTorIpsFragment)
    fun inject(fragment: PreferencesTorFragment)
    fun inject(fragment: ProxyFragment)
    fun inject(fragment: PreferencesDNSCryptServers)
    fun inject(fragment: FirewallFragment)
    fun inject(fragment: BackupFragment)
    fun inject(fragment: ConfigEditorFragment)
    fun inject(fragment: PreferencesCommonFragment)
    fun inject(fragment: PreferencesITPDFragment)
    fun inject(fragment: PreferencesDNSCryptRelays)
    fun inject(fragment: PreferencesDNSFragment)
    fun inject(fragment: ITPDSubscriptionsFragment)
    fun inject(fragment: UpdateModulesDialogFragment)
    fun inject(fragment: NotificationHelper)
    fun inject(fragment: DnsRulesFragment)
    fun inject(service: ModulesService)
    fun inject(service: RootExecService)
    fun inject(service: UpdateService)
    fun inject(receiver: BootCompleteReceiver)
    fun inject(receiver: DNSCryptFragmentReceiver)
    fun inject(receiver: TorFragmentReceiver)
    fun inject(receiver: ITPDFragmentReceiver)
    fun inject(receiver: HelpActivityReceiver)
    fun inject(receiver: IptablesReceiver)
    fun inject(dialogFragment: RequestIgnoreBatteryOptimizationDialog)
    fun inject(dialogFragment: RequestIgnoreDataRestrictionDialog)
    fun inject(dialogFragment: SendCrashReport)
    fun inject(dialogFragment: AddDNSCryptServerDialogFragment)
    fun inject(dialogFragment: AskRestoreDefaultsDialog)
    fun inject(dialogFragment: PleaseWaitDialogBridgesRequest)
    fun inject(dialogFragment: BridgesCaptchaDialogFragment)
    fun inject(dialogFragment: BridgesReadyDialogFragment)
    fun inject(dialogFragment: SelectBridgesTransportDialogFragment)
    fun inject(usageStatistic: UsageStatistic)
    fun inject(modulesKiller: ModulesKiller)
    fun inject(contextUIDUpdater: ContextUIDUpdater)
    fun inject(updateCheck: UpdateCheck)
    fun inject(downloadTask: DownloadTask)
    fun inject(torRefreshIPsWork: TorRefreshIPsWork)
    fun inject(fileManager: FileManager)
    fun inject(verifier: Verifier)
    fun inject(bridgeAdapter: BridgeAdapter)
    fun inject(modulesStarterHelper: ModulesStarterHelper)
    fun inject(backupHelper: BackupHelper)
    fun inject(tethering: Tethering)
    fun inject(serviceVPNHandler: ServiceVPNHandler)
    fun inject(installer: Installer)
    fun inject(installedApplicationsManager: InstalledApplicationsManager)
    fun inject(agreementDialog: AgreementDialog)
    fun inject(rulesEraser: RulesEraser)
    fun inject(worker: UpdateRemoteDnsRulesWorker)
    fun inject(worker: UpdateLocalDnsRulesWorker)
    fun inject(worker: RemixExistingDnsRulesWorker)
    fun inject(parser: DNSCryptLogParser)
    fun inject(parser: TorLogParser)
}
