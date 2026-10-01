/**
 * AEGIS BANKING & PAYMENT TRANSACTION SYSTEM
 * Frontend Application Client
 */

// Application State
const state = {
  token: localStorage.getItem('aegis_token') || null,
  user: JSON.parse(localStorage.getItem('aegis_user') || 'null'),
  accounts: [],
  currentPage: 0,
  pageSize: 10,
  totalPages: 1,
  totalElements: 0,
  currentTransactions: []
};

// ============================================================================
// INITIALIZATION
// ============================================================================
document.addEventListener('DOMContentLoaded', () => {
  if (state.token && state.user) {
    showDashboard();
    loadDashboardData();
  } else {
    showAuth();
  }
});

// ============================================================================
// AUTHENTICATION & SESSION MANAGEMENT
// ============================================================================
function showAuth() {
  document.getElementById('auth-section').classList.remove('hidden');
  document.getElementById('dashboard-section').classList.add('hidden');
  document.getElementById('user-nav-profile').classList.add('hidden');
}

function showDashboard() {
  document.getElementById('auth-section').classList.add('hidden');
  document.getElementById('dashboard-section').classList.remove('hidden');
  document.getElementById('user-nav-profile').classList.remove('hidden');

  if (state.user) {
    const initials = (state.user.firstName ? state.user.firstName[0] : '') +
                     (state.user.lastName ? state.user.lastName[0] : '') ||
                     state.user.username.substring(0, 2).toUpperCase();
    document.getElementById('user-avatar-initials').textContent = initials;
    document.getElementById('user-display-name').textContent =
      state.user.firstName ? `${state.user.firstName} ${state.user.lastName}` : state.user.username;
    document.getElementById('user-role-badge').textContent = state.user.role || 'ROLE_USER';
  }
}

function switchAuthTab(tab) {
  const loginTab = document.getElementById('tab-login');
  const registerTab = document.getElementById('tab-register');
  const loginForm = document.getElementById('login-form');
  const registerForm = document.getElementById('register-form');

  if (tab === 'login') {
    loginTab.classList.add('active');
    registerTab.classList.remove('active');
    loginForm.classList.remove('hidden');
    registerForm.classList.add('hidden');
  } else {
    registerTab.classList.add('active');
    loginTab.classList.remove('active');
    registerForm.classList.remove('hidden');
    loginForm.classList.add('hidden');
  }
}

function fillDemoCredentials() {
  switchAuthTab('login');
  document.getElementById('login-username').value = 'john_doe';
  document.getElementById('login-password').value = 'password123';
  showToast('Demo credentials inserted. Click "Authenticate Session" or Register if new.', 'info');
}

async function handleLogin(e) {
  e.preventDefault();
  const usernameOrEmail = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;

  const btn = document.getElementById('btn-submit-login');
  setButtonLoading(btn, true);

  try {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usernameOrEmail, password })
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Login failed. Please check credentials.');
    }

    // Save token and user details
    state.token = data.accessToken;
    state.user = {
      id: data.userId,
      username: data.username,
      email: data.email,
      role: data.role
    };

    localStorage.setItem('aegis_token', state.token);
    localStorage.setItem('aegis_user', JSON.stringify(state.user));

    showToast(`Welcome back, ${state.user.username}!`, 'success');
    showDashboard();
    await loadDashboardData();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setButtonLoading(btn, false);
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const firstName = document.getElementById('reg-first-name').value.trim();
  const lastName = document.getElementById('reg-last-name').value.trim();
  const username = document.getElementById('reg-username').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;

  const btn = document.getElementById('btn-submit-register');
  setButtonLoading(btn, true);

  try {
    const res = await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ firstName, lastName, username, email, password })
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Registration failed.');
    }

    state.token = data.accessToken;
    state.user = {
      id: data.userId,
      username: data.username,
      email: data.email,
      role: data.role,
      firstName,
      lastName
    };

    localStorage.setItem('aegis_token', state.token);
    localStorage.setItem('aegis_user', JSON.stringify(state.user));

    showToast('Profile created successfully! Initializing checking account...', 'success');
    showDashboard();

    // Automatically open a default checking account with initial deposit
    try {
      await apiRequest('/api/v1/accounts/me', 'POST', {
        type: 'CHECKING',
        initialBalance: 5000.00
      });
      showToast('Opening credit of $5,000.00 added to your new Checking account!', 'success');
    } catch (ignore) {
      // User can manually create account if needed
    }

    await loadDashboardData();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setButtonLoading(btn, false);
  }
}

document.getElementById('logout-btn')?.addEventListener('click', () => {
  localStorage.removeItem('aegis_token');
  localStorage.removeItem('aegis_user');
  state.token = null;
  state.user = null;
  state.accounts = [];
  showToast('Logged out successfully.', 'info');
  showAuth();
});

// ============================================================================
// API HELPER WITH AUTOMATIC BEARER AUTH
// ============================================================================
async function apiRequest(endpoint, method = 'GET', body = null, headers = {}) {
  const reqHeaders = {
    'Content-Type': 'application/json',
    ...headers
  };

  if (state.token) {
    reqHeaders['Authorization'] = `Bearer ${state.token}`;
  }

  const options = {
    method,
    headers: reqHeaders
  };

  if (body && (method === 'POST' || method === 'PUT')) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(endpoint, options);

  if (response.status === 401) {
    // Session expired
    localStorage.removeItem('aegis_token');
    state.token = null;
    showToast('Session expired. Please log in again.', 'error');
    showAuth();
    throw new Error('Unauthorized');
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const errorMsg = data && (data.message || data.error) ? (data.message || data.error) : `Error: ${response.statusText}`;
    throw new Error(errorMsg);
  }

  return data;
}

// ============================================================================
// DASHBOARD DATA LOADER
// ============================================================================
async function loadDashboardData() {
  try {
    // 1. Fetch user profile
    try {
      const profile = await apiRequest('/api/v1/users/me');
      state.user = { ...state.user, ...profile };
      localStorage.setItem('aegis_user', JSON.stringify(state.user));
      showDashboard();
    } catch (e) {
      console.warn('Could not fetch user profile:', e);
    }

    // 2. Fetch Accounts
    await fetchAccounts();

    // 3. Fetch Transactions
    await fetchTransactions(0);
  } catch (err) {
    console.error('Error loading dashboard data:', err);
    showToast('Failed to load banking data: ' + err.message, 'error');
  }
}

async function fetchAccounts() {
  try {
    const accounts = await apiRequest('/api/v1/accounts/me');
    state.accounts = accounts || [];
    renderAccounts(state.accounts);
    updateMetrics();
    populateAccountSelects();
  } catch (err) {
    console.error('Failed to fetch accounts:', err);
  }
}

function renderAccounts(accounts) {
  const container = document.getElementById('accounts-container');
  if (!accounts || accounts.length === 0) {
    container.innerHTML = `
      <div class="accounts-loading-skeleton" style="padding: 40px; border: 1px dashed var(--border-subtle); border-radius: var(--radius-lg);">
        <p style="font-size: 1.1rem; color: var(--text-light); margin-bottom: 12px;">No active bank accounts found.</p>
        <button class="btn btn-primary btn-sm" onclick="openNewAccountModal()">+ Open Your First Account</button>
      </div>
    `;
    return;
  }

  container.innerHTML = accounts.map(acc => {
    const isChecking = acc.type === 'CHECKING';
    const cardClass = isChecking ? 'card-checking' : 'card-savings';
    const formattedBalance = formatCurrency(acc.balance);

    return `
      <div class="account-card ${cardClass}">
        <div class="card-top">
          <span class="card-badge">${acc.type}</span>
          <div class="card-chip"></div>
        </div>

        <div class="card-balance-block">
          <span class="card-balance-label">Available Balance</span>
          <div class="card-balance-amount">${formattedBalance}</div>
        </div>

        <div class="card-bottom">
          <div class="card-acc-num-wrapper">
            <span class="card-acc-number">${acc.accountNumber}</span>
            <button class="btn-copy-acc" onclick="copyToClipboard('${acc.accountNumber}', 'Account number')" title="Copy Account Number">
              Copy
            </button>
          </div>
          <div class="card-quick-actions">
            <button class="card-action-btn" onclick="openTransferModal('${acc.accountNumber}')">Transfer</button>
            <button class="card-action-btn" onclick="openDepositModal('${acc.accountNumber}')">Deposit</button>
            <button class="card-action-btn" onclick="openWithdrawModal('${acc.accountNumber}')">Withdraw</button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function updateMetrics() {
  const totalBalance = state.accounts.reduce((sum, acc) => sum + parseFloat(acc.balance || 0), 0);
  document.getElementById('metric-total-balance').textContent = formatCurrency(totalBalance);
  document.getElementById('metric-accounts-count').textContent = `${state.accounts.length} Active Account${state.accounts.length === 1 ? '' : 's'}`;
}

function populateAccountSelects() {
  const selects = [
    document.getElementById('transfer-source'),
    document.getElementById('deposit-account'),
    document.getElementById('withdraw-account'),
    document.getElementById('filter-account')
  ];

  selects.forEach(select => {
    if (!select) return;
    const isFilter = select.id === 'filter-account';
    const currentValue = select.value;

    select.innerHTML = isFilter ? '<option value="">All Accounts</option>' : '';

    state.accounts.forEach(acc => {
      const opt = document.createElement('option');
      opt.value = acc.accountNumber;
      opt.textContent = `${acc.accountNumber} (${acc.type} - ${formatCurrency(acc.balance)})`;
      select.appendChild(opt);
    });

    if (currentValue) {
      select.value = currentValue;
    }
  });

  updateTransferAvailableBalance();
  updateWithdrawAvailableBalance();
}

// ============================================================================
// TRANSACTIONS & STATEMENTS
// ============================================================================
async function fetchTransactions(page = 0) {
  state.currentPage = page;

  const accountNumber = document.getElementById('filter-account')?.value || '';
  const type = document.getElementById('filter-type')?.value || '';
  const status = document.getElementById('filter-status')?.value || '';
  const startDateVal = document.getElementById('filter-start-date')?.value;
  const endDateVal = document.getElementById('filter-end-date')?.value;

  const params = new URLSearchParams({
    page: page,
    size: state.pageSize,
    sort: 'createdAt,desc'
  });

  if (accountNumber) params.append('accountNumber', accountNumber);
  if (type) params.append('type', type);
  if (status) params.append('status', status);
  if (startDateVal) params.append('startDate', `${startDateVal}T00:00:00`);
  if (endDateVal) params.append('endDate', `${endDateVal}T23:59:59`);

  const tbody = document.getElementById('transactions-tbody');
  tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Retrieving ledger events...</td></tr>`;

  try {
    const data = await apiRequest(`/api/v1/transactions/history?${params.toString()}`);

    const transactions = data.content || [];
    state.currentTransactions = transactions;
    state.totalPages = data.totalPages || 1;
    state.totalElements = data.totalElements || 0;

    renderTransactionsTable(transactions);
    updatePaginationControls();
    calculateInflowOutflow(transactions);
  } catch (err) {
    console.error('Failed to load transactions:', err);
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger">Failed to load transactions: ${err.message}</td></tr>`;
  }
}

function renderTransactionsTable(transactions) {
  const tbody = document.getElementById('transactions-tbody');

  if (!transactions || transactions.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No transactions found matching your criteria.</td></tr>`;
    return;
  }

  tbody.innerHTML = transactions.map(txn => {
    const typeBadge = getTypeBadge(txn.type);
    const statusBadge = `<span class="badge-status ${txn.status}"><span class="status-dot"></span>${txn.status}</span>`;
    const formattedDate = formatDate(txn.createdAt);
    
    // Check direction relative to user accounts
    const isOutgoing = state.accounts.some(acc => acc.accountNumber === txn.sourceAccountNumber);
    const isIncoming = state.accounts.some(acc => acc.accountNumber === txn.destinationAccountNumber);

    let amountDisplay = formatCurrency(txn.amount);
    let amountClass = 'text-light';

    if (txn.type === 'DEPOSIT' || (!isOutgoing && isIncoming)) {
      amountDisplay = `+${amountDisplay}`;
      amountClass = 'text-success';
    } else if (txn.type === 'WITHDRAWAL' || (isOutgoing && !isIncoming)) {
      amountDisplay = `-${amountDisplay}`;
      amountClass = 'text-danger';
    }

    const routeText = txn.type === 'TRANSFER'
      ? `${txn.sourceAccountNumber} → ${txn.destinationAccountNumber}`
      : (txn.type === 'DEPOSIT' ? `Deposit to ${txn.destinationAccountNumber}` : `Withdraw from ${txn.sourceAccountNumber}`);

    return `
      <tr>
        <td>
          <span class="ref-number" onclick="copyToClipboard('${txn.referenceNumber}', 'Reference')" title="Click to copy" style="cursor: pointer;">
            ${txn.referenceNumber}
          </span>
        </td>
        <td>${typeBadge}</td>
        <td><span style="font-family: var(--font-mono); font-size: 0.85rem;">${routeText}</span></td>
        <td class="amount-cell ${amountClass}">${amountDisplay}</td>
        <td>${statusBadge}</td>
        <td style="color: var(--text-muted); font-size: 0.82rem;">${formattedDate}</td>
      </tr>
    `;
  }).join('');
}

function getTypeBadge(type) {
  switch (type) {
    case 'TRANSFER':
      return `<span class="badge-type badge-transfer">⇄ Transfer</span>`;
    case 'DEPOSIT':
      return `<span class="badge-type badge-deposit">↓ Deposit</span>`;
    case 'WITHDRAWAL':
      return `<span class="badge-type badge-withdrawal">↑ Withdrawal</span>`;
    default:
      return `<span class="badge-type">${type}</span>`;
  }
}

function updatePaginationControls() {
  const info = document.getElementById('pagination-info');
  const indicator = document.getElementById('page-indicator');
  const btnPrev = document.getElementById('btn-prev-page');
  const btnNext = document.getElementById('btn-next-page');

  info.textContent = `Showing ${state.currentTransactions.length} of ${state.totalElements} records`;
  indicator.textContent = `Page ${state.currentPage + 1} of ${Math.max(1, state.totalPages)}`;

  btnPrev.disabled = state.currentPage <= 0;
  btnNext.disabled = state.currentPage >= state.totalPages - 1;
}

function changePage(direction) {
  const newPage = state.currentPage + direction;
  if (newPage >= 0 && newPage < state.totalPages) {
    fetchTransactions(newPage);
  }
}

function resetFilters() {
  document.getElementById('filter-account').value = '';
  document.getElementById('filter-type').value = '';
  document.getElementById('filter-status').value = '';
  document.getElementById('filter-start-date').value = '';
  document.getElementById('filter-end-date').value = '';
  fetchTransactions(0);
}

function calculateInflowOutflow(transactions) {
  let inflow = 0;
  let outflow = 0;

  transactions.forEach(t => {
    if (t.status !== 'COMPLETED') return;
    const isOutgoing = state.accounts.some(acc => acc.accountNumber === t.sourceAccountNumber);
    const isIncoming = state.accounts.some(acc => acc.accountNumber === t.destinationAccountNumber);

    if (t.type === 'DEPOSIT' || (!isOutgoing && isIncoming)) {
      inflow += parseFloat(t.amount || 0);
    } else if (t.type === 'WITHDRAWAL' || (isOutgoing && !isIncoming)) {
      outflow += parseFloat(t.amount || 0);
    }
  });

  document.getElementById('metric-inflow').textContent = `+${formatCurrency(inflow)}`;
  document.getElementById('metric-outflow').textContent = `-${formatCurrency(outflow)}`;
}

function exportTransactionsCsv() {
  if (!state.currentTransactions || state.currentTransactions.length === 0) {
    showToast('No transactions to export in current view.', 'info');
    return;
  }

  const headers = ['Reference Number', 'Type', 'Source Account', 'Destination Account', 'Amount', 'Status', 'Timestamp'];
  const rows = state.currentTransactions.map(t => [
    t.referenceNumber,
    t.type,
    t.sourceAccountNumber || '',
    t.destinationAccountNumber || '',
    t.amount,
    t.status,
    t.createdAt
  ]);

  let csvContent = 'data:text/csv;charset=utf-8,' +
    [headers.join(','), ...rows.map(e => e.join(','))].join('\n');

  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `statement_${new Date().toISOString().slice(0, 10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  showToast('Account statement exported successfully to CSV!', 'success');
}

// ============================================================================
// MODAL CONTROLS & OPERATIONS
// ============================================================================
function openModal(id) {
  document.getElementById(id).classList.remove('hidden');
}

function closeModal(id) {
  document.getElementById(id).classList.add('hidden');
}

function handleModalOverlayClick(event, id) {
  if (event.target.id === id) {
    closeModal(id);
  }
}

// 1. Transfer Modal
function openTransferModal(defaultSource = null) {
  if (state.accounts.length === 0) {
    showToast('Please open a bank account first.', 'warning');
    return;
  }
  const select = document.getElementById('transfer-source');
  if (defaultSource) select.value = defaultSource;
  regenerateIdempotencyKey();
  updateTransferAvailableBalance();
  document.getElementById('transfer-amount').value = '';
  document.getElementById('transfer-destination').value = '';
  openModal('modal-transfer');
}

function updateTransferAvailableBalance() {
  const accNum = document.getElementById('transfer-source').value;
  const acc = state.accounts.find(a => a.accountNumber === accNum);
  const hint = document.getElementById('transfer-available-balance');
  if (acc) {
    hint.textContent = `Available Balance: ${formatCurrency(acc.balance)}`;
  }
}

function setTransferAmount(amount) {
  document.getElementById('transfer-amount').value = amount.toFixed(2);
}

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

function regenerateIdempotencyKey() {
  document.getElementById('transfer-idempotency-key').value = generateUUID();
}

async function executeTransfer(e) {
  e.preventDefault();
  const sourceAccountNumber = document.getElementById('transfer-source').value;
  const destinationAccountNumber = document.getElementById('transfer-destination').value.trim();
  const amount = parseFloat(document.getElementById('transfer-amount').value);
  const idempotencyKey = document.getElementById('transfer-idempotency-key').value;

  if (sourceAccountNumber === destinationAccountNumber) {
    showToast('Source and destination accounts must be different.', 'error');
    return;
  }

  const btn = document.getElementById('btn-submit-transfer');
  setButtonLoading(btn, true);

  try {
    const res = await apiRequest('/api/v1/transactions/transfer', 'POST', {
      sourceAccountNumber,
      destinationAccountNumber,
      amount,
      idempotencyKey
    }, {
      'X-Idempotency-Key': idempotencyKey
    });

    closeModal('modal-transfer');
    showToast(`Transfer of ${formatCurrency(amount)} successful! Ref: ${res.referenceNumber}`, 'success');

    // Refresh accounts & transactions
    await fetchAccounts();
    await fetchTransactions(0);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setButtonLoading(btn, false);
  }
}

// 2. Deposit Modal
function openDepositModal(defaultAccount = null) {
  if (state.accounts.length === 0) {
    showToast('Please open a bank account first.', 'warning');
    return;
  }
  const select = document.getElementById('deposit-account');
  if (defaultAccount) select.value = defaultAccount;
  document.getElementById('deposit-amount').value = '';
  openModal('modal-deposit');
}

function setDepositAmount(amount) {
  document.getElementById('deposit-amount').value = amount.toFixed(2);
}

async function executeDeposit(e) {
  e.preventDefault();
  const accountNumber = document.getElementById('deposit-account').value;
  const amount = parseFloat(document.getElementById('deposit-amount').value);

  const btn = document.getElementById('btn-submit-deposit');
  setButtonLoading(btn, true);

  try {
    await apiRequest(`/api/v1/accounts/${accountNumber}/deposit`, 'POST', { amount });
    closeModal('modal-deposit');
    showToast(`Successfully deposited ${formatCurrency(amount)}!`, 'success');
    await fetchAccounts();
    await fetchTransactions(0);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setButtonLoading(btn, false);
  }
}

// 3. Withdraw Modal
function openWithdrawModal(defaultAccount = null) {
  if (state.accounts.length === 0) {
    showToast('Please open a bank account first.', 'warning');
    return;
  }
  const select = document.getElementById('withdraw-account');
  if (defaultAccount) select.value = defaultAccount;
  updateWithdrawAvailableBalance();
  document.getElementById('withdraw-amount').value = '';
  openModal('modal-withdraw');
}

function updateWithdrawAvailableBalance() {
  const accNum = document.getElementById('withdraw-account').value;
  const acc = state.accounts.find(a => a.accountNumber === accNum);
  const hint = document.getElementById('withdraw-available-balance');
  if (acc) {
    hint.textContent = `Available Balance: ${formatCurrency(acc.balance)}`;
  }
}

function setWithdrawAmount(amount) {
  document.getElementById('withdraw-amount').value = amount.toFixed(2);
}

async function executeWithdraw(e) {
  e.preventDefault();
  const accountNumber = document.getElementById('withdraw-account').value;
  const amount = parseFloat(document.getElementById('withdraw-amount').value);

  const btn = document.getElementById('btn-submit-withdraw');
  setButtonLoading(btn, true);

  try {
    await apiRequest(`/api/v1/accounts/${accountNumber}/withdraw`, 'POST', { amount });
    closeModal('modal-withdraw');
    showToast(`Successfully withdrew ${formatCurrency(amount)}!`, 'success');
    await fetchAccounts();
    await fetchTransactions(0);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setButtonLoading(btn, false);
  }
}

// 4. Open New Account Modal
function openNewAccountModal() {
  document.getElementById('new-account-initial-deposit').value = '100.00';
  openModal('modal-new-account');
}

async function executeOpenAccount(e) {
  e.preventDefault();
  const type = document.querySelector('input[name="account-type"]:checked').value;
  const initialBalance = parseFloat(document.getElementById('new-account-initial-deposit').value || 0);

  const btn = document.getElementById('btn-submit-new-account');
  setButtonLoading(btn, true);

  try {
    const res = await apiRequest('/api/v1/accounts/me', 'POST', {
      type,
      initialBalance
    });

    closeModal('modal-new-account');
    showToast(`New ${type} account ${res.accountNumber} created!`, 'success');
    await fetchAccounts();
    await fetchTransactions(0);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setButtonLoading(btn, false);
  }
}

// ============================================================================
// UTILITIES
// ============================================================================
function formatCurrency(amount) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2
  }).format(amount || 0);
}

function formatDate(dateStr) {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function copyToClipboard(text, label = 'Text') {
  navigator.clipboard.writeText(text).then(() => {
    showToast(`${label} copied to clipboard!`, 'info');
  }).catch(() => {
    showToast(`Could not copy to clipboard.`, 'error');
  });
}

function setButtonLoading(button, isLoading) {
  if (!button) return;
  const textSpan = button.querySelector('.btn-text');
  const spinnerSpan = button.querySelector('.btn-spinner');

  if (isLoading) {
    button.disabled = true;
    if (textSpan) textSpan.classList.add('hidden');
    if (spinnerSpan) spinnerSpan.classList.remove('hidden');
  } else {
    button.disabled = false;
    if (textSpan) textSpan.classList.remove('hidden');
    if (spinnerSpan) spinnerSpan.classList.add('hidden');
  }
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  let icon = 'ℹ️';
  if (type === 'success') icon = '✓';
  if (type === 'error') icon = '⚠';

  toast.innerHTML = `
    <span style="font-weight: 700; font-size: 1.1rem;">${icon}</span>
    <span>${message}</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}
