package hopper.pfm.insect.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.ui.PfmBanner
import hopper.pfm.insect.core.ui.PfmPasswordField
import hopper.pfm.insect.core.ui.PfmPrimaryButton
import hopper.pfm.insect.core.ui.PfmPrompt
import hopper.pfm.insect.core.ui.PfmTextField
import hopper.pfm.insect.core.ui.PfmTextLink

@Composable
private fun AuthLayout(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun Logo() {
    Box(
        modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun LoginScreen(onRegister: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AuthLayout {
        Spacer(Modifier.height(48.dp))
        Logo()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.app_name_auth), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.auth_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
        Spacer(Modifier.height(40.dp))
        state.error?.let {
            PfmBanner(stringResource(it.res), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(16.dp))
        }
        PfmTextField(
            value = state.email,
            onValueChange = viewModel::onEmail,
            label = stringResource(R.string.auth_email),
            errorText = state.emailError?.let { stringResource(it.res) },
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(8.dp))
        PfmPasswordField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            label = stringResource(R.string.auth_password),
            showDescription = stringResource(R.string.auth_show_password),
            hideDescription = stringResource(R.string.auth_hide_password),
            errorText = state.passwordError?.let { stringResource(it.res) },
            imeAction = ImeAction.Done,
            onImeAction = viewModel::submit,
        )
        Spacer(Modifier.height(24.dp))
        PfmPrimaryButton(stringResource(R.string.auth_login), onClick = viewModel::submit, loading = state.loading)
        Spacer(Modifier.height(16.dp))
        PfmPrompt(stringResource(R.string.auth_no_account), stringResource(R.string.auth_register), onRegister)
    }
}

@Composable
fun RegisterScreen(onLogin: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val check = state.passwordCheck
    AuthLayout {
        Text(
            stringResource(R.string.auth_create_account),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        )
        Text(
            stringResource(R.string.auth_create_account_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
        )
        state.error?.let {
            PfmBanner(stringResource(it.res), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(16.dp))
        }
        PfmTextField(
            value = state.email,
            onValueChange = viewModel::onEmail,
            label = stringResource(R.string.auth_email),
            errorText = state.emailError?.let { stringResource(it.res) },
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(8.dp))
        PfmPasswordField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            label = stringResource(R.string.auth_password),
            showDescription = stringResource(R.string.auth_show_password),
            hideDescription = stringResource(R.string.auth_hide_password),
        )
        Spacer(Modifier.height(8.dp))
        PfmPasswordField(
            value = state.confirm,
            onValueChange = viewModel::onConfirm,
            label = stringResource(R.string.auth_confirm_password),
            showDescription = stringResource(R.string.auth_show_password),
            hideDescription = stringResource(R.string.auth_hide_password),
            errorText = state.confirmError?.let { stringResource(it.res) },
            imeAction = ImeAction.Done,
            onImeAction = viewModel::submit,
        )
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Rule(stringResource(R.string.auth_rule_length), check.longEnough)
            Rule(stringResource(R.string.auth_rule_case), check.hasLowerAndUpper)
            Rule(stringResource(R.string.auth_rule_digit), check.hasDigit)
        }
        Spacer(Modifier.height(24.dp))
        PfmPrimaryButton(stringResource(R.string.auth_register), onClick = viewModel::submit, loading = state.loading)
        Spacer(Modifier.height(16.dp))
        PfmPrompt(stringResource(R.string.auth_have_account), stringResource(R.string.auth_login), onLogin)
    }
}

@Composable
private fun Rule(text: String, met: Boolean) {
    val color = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (met) Icons.Filled.Check else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
fun SessionExpiredScreen(viewModel: ReloginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    AuthLayout {
        PfmBanner(stringResource(R.string.auth_expired_banner), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.height(40.dp))
        Logo()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.auth_expired_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.auth_expired_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 32.dp),
        )
        PfmTextField(value = state.email, onValueChange = {}, label = stringResource(R.string.auth_email), enabled = false)
        Spacer(Modifier.height(8.dp))
        PfmPasswordField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            label = stringResource(R.string.auth_password),
            showDescription = stringResource(R.string.auth_show_password),
            hideDescription = stringResource(R.string.auth_hide_password),
            errorText = state.error?.let { stringResource(it.res) },
            imeAction = ImeAction.Done,
            onImeAction = viewModel::submit,
        )
        Spacer(Modifier.height(24.dp))
        PfmPrimaryButton(stringResource(R.string.auth_login), onClick = viewModel::submit, loading = state.loading)
        Spacer(Modifier.height(8.dp))
        PfmTextLink(stringResource(R.string.auth_other_account), onClick = { confirmDiscard = true }, destructive = true)
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.auth_discard_title)) },
            text = { Text(stringResource(R.string.auth_discard_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; viewModel.discardLocalData() }) {
                    Text(stringResource(R.string.auth_discard_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}
