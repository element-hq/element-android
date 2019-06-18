/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.core.di

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dagger.BindsInstance
import dagger.Component
import im.vector.riotredesign.core.platform.SimpleFragmentActivity
import im.vector.riotredesign.features.crypto.keysbackup.settings.KeysBackupSettingsFragment
import im.vector.riotredesign.features.home.HomeActivity
import im.vector.riotredesign.features.home.HomeModule
import im.vector.riotredesign.features.home.room.detail.RoomDetailFragment
import im.vector.riotredesign.features.roomdirectory.picker.RoomDirectoryPickerFragment
import im.vector.riotredesign.features.roomdirectory.roompreview.RoomPreviewNoPreviewFragment

@Component(dependencies = [VectorComponent::class], modules = [ViewModelModule::class, HomeModule::class])
@ScreenScope
interface ScreenComponent {

    fun viewModelFactory(): ViewModelProvider.Factory

    fun inject(activity: SimpleFragmentActivity)

    fun inject(activity: HomeActivity)

    fun inject(roomDetailFragment: RoomDetailFragment)

    fun inject(roomDirectoryPickerFragment: RoomDirectoryPickerFragment)

    fun inject(roomPreviewNoPreviewFragment: RoomPreviewNoPreviewFragment)
    
    fun inject(keysBackupSettingsFragment: KeysBackupSettingsFragment)

    @Component.Factory
    interface Factory {
        fun create(vectorComponent: VectorComponent,
                   @BindsInstance context: AppCompatActivity
        ): ScreenComponent
    }
}
