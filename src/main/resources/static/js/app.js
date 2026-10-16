/**
 * Smart Food Expiry Management System - JavaScript Application Driver
 * Features:
 *  - USB Hardware Barcode Scanner Listener
 *  - Camera Barcode Scanner (html5-qrcode)
 *  - Direct Backend Storing from Scanner (Quick Batch & Quick Register)
 *  - 20 Demo Barcodes Interactive Simulator
 *  - Interactive Chart.js Rendering
 */

document.addEventListener('DOMContentLoaded', function () {
    initUsbBarcodeScanner();
    initDashboardCharts();
});

function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.content;
    return token && headerName ? { [headerName]: token } : {};
}

/* ===================================================================
   1. USB Hardware Barcode Scanner Listener
   Hardware scanners simulate rapid keystrokes ending in Enter.
   =================================================================== */
let barcodeBuffer = '';
let lastKeyTime = Date.now();

function initUsbBarcodeScanner() {
    document.addEventListener('keydown', function (e) {
        const activeElem = document.activeElement;
        const isTextInput = activeElem && (
            (activeElem.tagName === 'INPUT' && activeElem.id !== 'modalBarcodeInput' && activeElem.id !== 'barcode') ||
            activeElem.tagName === 'TEXTAREA'
        );

        const currentTime = Date.now();
        const diff = currentTime - lastKeyTime;
        lastKeyTime = currentTime;

        // Hardware scanners output characters with very low latency (< 40ms)
        if (e.key === 'Enter') {
            if (barcodeBuffer.length >= 4 && diff < 80) {
                e.preventDefault();
                const scannedBarcode = barcodeBuffer.trim();
                barcodeBuffer = '';
                handleScannedBarcode(scannedBarcode);
            } else {
                barcodeBuffer = '';
            }
        } else if (e.key.length === 1 && !isTextInput) {
            if (diff > 120 && barcodeBuffer.length > 0) {
                barcodeBuffer = '';
            }
            barcodeBuffer += e.key;
        }
    });
}

/* ===================================================================
   2. Barcode Handling & Lookup Workflow
   =================================================================== */
function handleScannedBarcode(barcode) {
    if (!barcode) return;

    // Fill direct barcode input if present on the active page
    const directBarcodeInput = document.getElementById('barcode');
    if (directBarcodeInput) {
        directBarcodeInput.value = barcode;
    }

    // Open Barcode Lookup Modal
    const barcodeModalElem = document.getElementById('barcodeScannerModal');
    if (barcodeModalElem) {
        const modal = bootstrap.Modal.getOrCreateInstance(barcodeModalElem);
        modal.show();

        // Switch to Scanner tab
        const scannerTabBtn = document.getElementById('scanner-view-tab');
        if (scannerTabBtn) {
            bootstrap.Tab.getOrCreateInstance(scannerTabBtn).show();
        }
    }

    const modalInput = document.getElementById('modalBarcodeInput');
    if (modalInput) {
        modalInput.value = barcode;
    }

    playBeep();
    performBarcodeLookup(barcode);
}

function triggerManualBarcodeLookup() {
    const input = document.getElementById('modalBarcodeInput');
    if (input && input.value.trim()) {
        performBarcodeLookup(input.value.trim());
    }
}

function simulateScanBarcode(barcode) {
    const scannerTabBtn = document.getElementById('scanner-view-tab');
    if (scannerTabBtn) {
        bootstrap.Tab.getOrCreateInstance(scannerTabBtn).show();
    }
    const modalInput = document.getElementById('modalBarcodeInput');
    if (modalInput) {
        modalInput.value = barcode;
    }
    playBeep();
    performBarcodeLookup(barcode);
}

function switchToDemoTab() {
    const demoTabBtn = document.getElementById('demo-sheet-tab');
    if (demoTabBtn) {
        bootstrap.Tab.getOrCreateInstance(demoTabBtn).show();
        loadDemoBarcodesList();
    }
}

function performBarcodeLookup(barcode) {
    const resultContainer = document.getElementById('barcodeLookupResult');
    if (!resultContainer) return;

    resultContainer.innerHTML = `
        <div class="card border-0 bg-light text-center py-4">
            <div class="spinner-border text-success mx-auto" role="status"></div>
            <p class="mt-2 text-muted fw-semibold">Connecting to backend & querying barcode: <strong>${escapeHtml(barcode)}</strong>...</p>
        </div>
    `;

    fetch(`/api/barcode/lookup?barcode=${encodeURIComponent(barcode)}`)
        .then(response => {
            if (!response.ok) throw new Error('Network error: ' + response.statusText);
            return response.json();
        })
        .then(data => {
            if (data.found) {
                renderProductFound(data, resultContainer);
            } else {
                renderProductNotFound(barcode, resultContainer);
            }
        })
        .catch(err => {
            resultContainer.innerHTML = `
                <div class="alert alert-danger shadow-sm">
                    <i class="bi bi-exclamation-octagon-fill me-2"></i> Error connecting to backend API: ${escapeHtml(err.message)}
                </div>
            `;
        });
}

function renderProductFound(data, container) {
    fillProductFormFromBarcode(data);
    let batchesHtml = '';
    if (data.existingBatches && data.existingBatches.length > 0) {
        batchesHtml = `
            <h6 class="fw-bold mt-3 mb-2 text-dark"><i class="bi bi-boxes me-1 text-success"></i> Existing Batches in Database:</h6>
            <div class="table-responsive mb-3">
                <table class="table table-sm table-bordered align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>Batch #</th>
                            <th>Current Stock</th>
                            <th>Expiry Date</th>
                            <th>Expiry Status</th>
                        </tr>
                    </thead>
                    <tbody id="modalBatchesList-${data.productId}">
                        ${data.existingBatches.map(b => `
                            <tr>
                                <td><strong>${escapeHtml(b.batchNumber)}</strong></td>
                                <td><span class="badge bg-success">${b.quantity} ${escapeHtml(data.unit || 'pcs')}</span></td>
                                <td>${escapeHtml(b.expiryDate)}</td>
                                <td><span class="badge bg-secondary">${escapeHtml(b.status)}</span></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } else {
        batchesHtml = `<div class="alert alert-light text-muted small my-2">No active stock batches found for this product. You can inward a batch below.</div>`;
    }

    const todayStr = new Date().toISOString().split('T')[0];
    const defaultBatchNum = 'BAT-' + Date.now().toString().slice(-6);

    container.innerHTML = `
        <div class="card border-success shadow-sm">
            <div class="card-header bg-success text-white d-flex justify-content-between align-items-center py-2">
                <span class="fw-semibold"><i class="bi bi-check-circle-fill me-1"></i> Product Found in Backend</span>
                <span class="badge bg-white text-success fw-bold">${escapeHtml(data.categoryName || 'General')}</span>
            </div>
            <div class="card-body">
                <div class="row align-items-center mb-3">
                    <div class="col-12 col-md-8">
                        <h4 class="card-title fw-bold text-dark mb-1">${escapeHtml(data.productName)}</h4>
                        <div class="text-muted small">
                            Brand: <strong>${escapeHtml(data.brand)}</strong> &bull;
                            Barcode: <code>${escapeHtml(data.barcode)}</code> &bull;
                            Unit: <strong>${escapeHtml(data.unit)}</strong>
                        </div>
                    </div>
                    <div class="col-12 col-md-4 text-md-end mt-2 mt-md-0">
                        <span class="text-muted small d-block">Selling Price</span>
                        <h4 class="fw-bold text-success mb-0">$${data.sellingPrice ? data.sellingPrice.toFixed(2) : '0.00'}</h4>
                    </div>
                </div>

                ${batchesHtml}

                <!-- Direct Inward Batch Form in Modal -->
                <div class="bg-light p-3 rounded-3 border">
                    <h6 class="fw-bold text-dark mb-2"><i class="bi bi-plus-circle-fill text-success me-1"></i> Scan & Store New Stock Batch to Backend</h6>
                    <form id="quickBatchForm-${data.productId}" onsubmit="submitQuickBatch(event, ${data.productId})">
                        <div class="row g-2">
                            <div class="col-12 col-sm-3">
                                <label class="form-label small fw-semibold mb-1">Batch #</label>
                                <input type="text" name="batchNumber" class="form-control form-control-sm" value="${defaultBatchNum}" required>
                            </div>
                            <div class="col-6 col-sm-2">
                                <label class="form-label small fw-semibold mb-1">Quantity</label>
                                <input type="number" min="1" name="quantity" class="form-control form-control-sm" placeholder="e.g. 20" required>
                            </div>
                            <div class="col-6 col-sm-3">
                                <label class="form-label small fw-semibold mb-1">Expiry Date</label>
                                <input type="date" name="expiryDate" class="form-control form-control-sm" min="${todayStr}" required>
                            </div>
                            <div class="col-12 col-sm-2">
                                <label class="form-label small fw-semibold mb-1">Supplier</label>
                                <input type="text" name="supplier" class="form-control form-control-sm" placeholder="e.g. Fresh Wholesale">
                            </div>
                            <div class="col-12 col-sm-2 d-flex align-items-end">
                                <button type="submit" class="btn btn-sm btn-emerald w-100 fw-semibold">
                                    <i class="bi bi-cloud-arrow-up-fill me-1"></i> Store Batch
                                </button>
                            </div>
                        </div>
                    </form>
                    <div id="quickBatchMsg-${data.productId}" class="mt-2 small"></div>
                </div>

                <div class="d-flex justify-content-end gap-2 mt-3 pt-2 border-top">
                    <a href="/products/view/${data.productId}" class="btn btn-sm btn-outline-secondary">
                        <i class="bi bi-eye me-1"></i> Open Product Page
                    </a>
                </div>
            </div>
        </div>
    `;
}

function fillProductFormFromBarcode(product) {
    const batchProductSelect = document.getElementById('productId');
    if (batchProductSelect) {
        const matchingOption = Array.from(batchProductSelect.options).find(option => option.dataset.barcode === product.barcode);
        if (matchingOption) {
            batchProductSelect.value = matchingOption.value;
            batchProductSelect.dispatchEvent(new Event('change', { bubbles: true }));
            if (typeof updateProductPrices === 'function') updateProductPrices();
        }
    }

    const form = document.querySelector('form[action="/products/save"], form[action$="/products/save"]');
    if (!form || !product) return;
    const setField = (selector, value) => {
        const field = form.querySelector(selector);
        if (field && value !== null && value !== undefined) {
            field.value = value;
            field.dispatchEvent(new Event('change', { bubbles: true }));
            field.dispatchEvent(new Event('input', { bubbles: true }));
        }
    };
    setField('[name="name"]', product.productName);
    setField('[name="category.id"]', product.categoryId);
    setField('[name="brand"]', product.brand);
    setField('[name="barcode"]', product.barcode);
    setField('[name="unit"]', product.unit);
    setField('[name="minStockQuantity"]', product.minStockQuantity);
    setField('[name="purchasePrice"]', product.purchasePrice);
    setField('[name="sellingPrice"]', product.sellingPrice);
    const message = document.getElementById('barcodeFormFillMessage');
    if (message) {
        message.hidden = false;
        message.textContent = 'Product details loaded from the backend. Review the values, then save.';
    }
}

function submitQuickBatch(event, productId) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    formData.append('productId', productId);

    const msgContainer = document.getElementById(`quickBatchMsg-${productId}`);
    msgContainer.innerHTML = `<span class="text-primary"><div class="spinner-border spinner-border-sm me-1"></div> Storing batch into database...</span>`;

    fetch('/api/barcode/quick-batch', {
        method: 'POST',
        headers: csrfHeaders(),
        body: formData
    })
    .then(res => res.json())
    .then(res => {
        if (res.success) {
            playBeep();
            msgContainer.innerHTML = `<span class="text-success fw-bold"><i class="bi bi-check-circle-fill me-1"></i> ${escapeHtml(res.message)}</span>`;
            form.reset();
            // Refresh lookup
            const input = document.getElementById('modalBarcodeInput');
            if (input && input.value) {
                setTimeout(() => performBarcodeLookup(input.value), 1000);
            }
        } else {
            msgContainer.innerHTML = `<span class="text-danger fw-bold"><i class="bi bi-exclamation-triangle-fill me-1"></i> ${escapeHtml(res.error || 'Failed to save')}</span>`;
        }
    })
    .catch(err => {
        msgContainer.innerHTML = `<span class="text-danger fw-bold"><i class="bi bi-exclamation-triangle-fill me-1"></i> ${escapeHtml(err.message)}</span>`;
    });
}

function renderProductNotFound(barcode, container) {
    const todayStr = new Date().toISOString().split('T')[0];
    const defaultBatchNum = 'BAT-' + Date.now().toString().slice(-6);

    container.innerHTML = `
        <div class="card border-warning shadow-sm">
            <div class="card-header bg-warning bg-opacity-25 text-dark fw-bold py-2">
                <i class="bi bi-exclamation-triangle-fill text-warning me-1"></i> Unregistered Barcode: <code>${escapeHtml(barcode)}</code>
            </div>
            <div class="card-body">
                <p class="text-muted small mb-3">
                    This barcode is not yet registered in the store catalog. Enter product and batch specifications below to save it to the inventory database.
                </p>

                <form id="quickRegisterForm" onsubmit="submitQuickRegister(event)">
                    <input type="hidden" name="barcode" value="${escapeHtml(barcode)}">
                    <div class="row g-2">
                        <div class="col-12 col-md-6">
                            <label class="form-label small fw-semibold mb-1">Product Name <span class="text-danger">*</span></label>
                            <input type="text" name="name" class="form-control form-control-sm" placeholder="e.g. Organic Almond Butter 250g" required>
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small fw-semibold mb-1">Category <span class="text-danger">*</span></label>
                            <select name="categoryId" class="form-select form-select-sm" required>
                                <option value="">Loading categories…</option>
                            </select>
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small fw-semibold mb-1">Brand <span class="text-danger">*</span></label>
                            <input type="text" name="brand" class="form-control form-control-sm" placeholder="Brand / Maker" required>
                        </div>

                        <div class="col-6 col-md-3">
                            <label class="form-label small fw-semibold mb-1">Unit</label>
                            <select name="unit" class="form-select form-select-sm">
                                <option value="pcs">Pieces (pcs)</option>
                                <option value="pack">Pack</option>
                                <option value="liters">Liters (L)</option>
                                <option value="kg">Kilograms (kg)</option>
                                <option value="bottles">Bottles</option>
                            </select>
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small fw-semibold mb-1">Selling Price ($)</label>
                            <input type="number" step="0.01" min="0" name="sellingPrice" class="form-control form-control-sm" placeholder="0.00">
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small fw-semibold mb-1">Purchase Price ($)</label>
                            <input type="number" step="0.01" min="0" name="purchasePrice" class="form-control form-control-sm" placeholder="0.00">
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small fw-semibold mb-1">Low Stock Alert</label>
                            <input type="number" min="0" name="minStockQuantity" class="form-control form-control-sm" value="10" required>
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small fw-semibold mb-1">Batch Number <span class="text-danger">*</span></label>
                            <input type="text" name="batchNumber" class="form-control form-control-sm" value="${defaultBatchNum}" required>
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small fw-semibold mb-1">Initial Stock <span class="text-danger">*</span></label>
                            <input type="number" min="1" name="quantity" class="form-control form-control-sm" value="20" required>
                        </div>

                        <div class="col-6 col-md-6">
                            <label class="form-label small fw-semibold mb-1">Expiration Date <span class="text-danger">*</span></label>
                            <input type="date" name="expiryDate" class="form-control form-control-sm" min="${todayStr}" required>
                        </div>
                        <div class="col-6 col-md-6">
                            <label class="form-label small fw-semibold mb-1">Supplier</label>
                            <input type="text" name="supplier" class="form-control form-control-sm" placeholder="Supplier / Distributor">
                        </div>
                    </div>

                    <div id="quickRegisterMsg" class="mt-2 small"></div>

                    <div class="d-flex justify-content-end gap-2 mt-3 pt-2 border-top">
                        <button type="submit" class="btn btn-emerald btn-sm px-3 shadow-sm">
                            <i class="bi bi-save2-fill me-1"></i> Store Product & Batch in Database
                        </button>
                    </div>
                </form>
            </div>
        </div>
    `;
    loadBarcodeCategories();
}

function loadBarcodeCategories() {
    const select = document.querySelector('#quickRegisterForm select[name="categoryId"]');
    if (!select) return;
    fetch('/api/barcode/categories')
        .then(response => {
            if (!response.ok) throw new Error('Could not load categories');
            return response.json();
        })
        .then(categories => {
            select.innerHTML = '<option value="">Select a category</option>' + categories.map(category =>
                `<option value="${category.id}">${escapeHtml(category.name)} (${category.warningDays}d warning)</option>`
            ).join('');
        })
        .catch(() => { select.innerHTML = '<option value="">Categories unavailable</option>'; });
}

function submitQuickRegister(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    const barcode = formData.get('barcode');

    const msgContainer = document.getElementById('quickRegisterMsg');
    msgContainer.innerHTML = `<span class="text-primary"><div class="spinner-border spinner-border-sm me-1"></div> Saving product and batch into database...</span>`;

    fetch('/api/barcode/quick-register', {
        method: 'POST',
        headers: csrfHeaders(),
        body: formData
    })
    .then(res => res.json())
    .then(res => {
        if (res.success) {
            playBeep();
            msgContainer.innerHTML = `<span class="text-success fw-bold"><i class="bi bi-check-circle-fill me-1"></i> ${escapeHtml(res.message)}</span>`;
            setTimeout(() => performBarcodeLookup(barcode), 1200);
        } else {
            msgContainer.innerHTML = `<span class="text-danger fw-bold"><i class="bi bi-exclamation-triangle-fill me-1"></i> ${escapeHtml(res.error || 'Failed to save')}</span>`;
        }
    })
    .catch(err => {
        msgContainer.innerHTML = `<span class="text-danger fw-bold"><i class="bi bi-exclamation-triangle-fill me-1"></i> ${escapeHtml(err.message)}</span>`;
    });
}

/* ===================================================================
   3. 20 Demo Barcodes Table Loader
   =================================================================== */
function loadDemoBarcodesList() {
    const tableBody = document.getElementById('demoBarcodesTableBody');
    if (!tableBody) return;

    fetch('/api/barcode/demo-list')
        .then(res => res.json())
        .then(items => {
            if (!items || items.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-3">No products found.</td></tr>`;
                return;
            }

            tableBody.innerHTML = items.map((p, idx) => `
                <tr>
                    <td><code>${escapeHtml(p.barcode)}</code></td>
                    <td class="fw-bold text-dark">${escapeHtml(p.name)}</td>
                    <td>${escapeHtml(p.brand)}</td>
                    <td><span class="badge bg-light text-dark">${escapeHtml(p.category)}</span></td>
                    <td>${escapeHtml(p.unit)}</td>
                    <td class="fw-semibold text-success">$${p.sellingPrice ? p.sellingPrice.toFixed(2) : '0.00'}</td>
                    <td class="text-end">
                        <button type="button" class="btn btn-sm btn-emerald py-0 px-2" onclick="simulateScanBarcode('${escapeHtml(p.barcode)}')">
                            <i class="bi bi-upc-scan me-1"></i> Test Scan
                        </button>
                    </td>
                </tr>
            `).join('');
        })
        .catch(err => {
            tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-3">Error loading demo barcodes: ${escapeHtml(err.message)}</td></tr>`;
        });
}

/* ===================================================================
   4. Camera-Based Barcode Scanner (html5-qrcode)
   =================================================================== */
let html5QrCodeInstance = null;

function toggleCameraScanner() {
    const readerElem = document.getElementById('qr-reader');
    if (!readerElem) return;

    if (html5QrCodeInstance && html5QrCodeInstance.isScanning) {
        stopCameraScanner();
        return;
    }

    readerElem.style.display = 'block';

    if (typeof Html5Qrcode === 'undefined') {
        alert('Html5Qrcode scanner library is loading. Please try again in a moment.');
        return;
    }

    html5QrCodeInstance = new Html5Qrcode("qr-reader");
    const config = { fps: 10, qrbox: { width: 280, height: 220 } };

    html5QrCodeInstance.start(
        { facingMode: "environment" },
        config,
        (decodedText, decodedResult) => {
            playBeep();
            stopCameraScanner();
            handleScannedBarcode(decodedText);
        },
        (errorMessage) => {}
    ).catch(err => {
        readerElem.innerHTML = `<div class="alert alert-danger mt-2 small">Camera permission denied or camera not found: ${err}</div>`;
    });
}

function stopCameraScanner() {
    const readerElem = document.getElementById('qr-reader');
    if (html5QrCodeInstance && html5QrCodeInstance.isScanning) {
        html5QrCodeInstance.stop().then(() => {
            if (readerElem) readerElem.style.display = 'none';
        }).catch(err => console.error("Error stopping scanner", err));
    } else if (readerElem) {
        readerElem.style.display = 'none';
    }
}

function playBeep() {
    try {
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.frequency.value = 880;
        gain.gain.value = 0.1;
        osc.start();
        setTimeout(() => {
            osc.stop();
            audioCtx.close();
        }, 120);
    } catch (e) {}
}

/* ===================================================================
   5. Chart.js Dashboard Visualizations
   =================================================================== */
function initDashboardCharts() {
    const expiryStatusCanvas = document.getElementById('expiryStatusChart');
    const categoryCanvas = document.getElementById('categoryStockChart');
    const upcomingCanvas = document.getElementById('upcomingExpiryChart');

    if (!expiryStatusCanvas && !categoryCanvas && !upcomingCanvas) {
        return;
    }

    fetch('/api/dashboard/stats')
        .then(res => res.json())
        .then(stats => {
            if (expiryStatusCanvas) {
                const statusLabels = Object.keys(stats.statusCounts || {});
                const statusData = Object.values(stats.statusCounts || {});

                new Chart(expiryStatusCanvas, {
                    type: 'doughnut',
                    data: {
                        labels: statusLabels,
                        datasets: [{
                            data: statusData,
                            backgroundColor: ['#16a34a', '#f59e0b', '#dc2626'],
                            borderWidth: 2,
                            borderColor: '#ffffff'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: { legend: { position: 'bottom' } },
                        cutout: '65%'
                    }
                });
            }

            if (categoryCanvas) {
                const catLabels = Object.keys(stats.categoryCounts || {});
                const catData = Object.values(stats.categoryCounts || {});

                new Chart(categoryCanvas, {
                    type: 'bar',
                    data: {
                        labels: catLabels,
                        datasets: [{
                            label: 'Valid Stock Units',
                            data: catData,
                            backgroundColor: '#10b981',
                            borderRadius: 6
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: { beginAtZero: true, grid: { color: '#f1f5f9' } },
                            x: { grid: { display: false } }
                        },
                        plugins: { legend: { display: false } }
                    }
                });
            }

            if (upcomingCanvas) {
                const upLabels = Object.keys(stats.upcomingExpiriesMap || {});
                const upData = Object.values(stats.upcomingExpiriesMap || {});

                new Chart(upcomingCanvas, {
                    type: 'line',
                    data: {
                        labels: upLabels,
                        datasets: [{
                            label: 'Batches Expiring (Units)',
                            data: upData,
                            borderColor: '#eab308',
                            backgroundColor: 'rgba(234, 179, 8, 0.15)',
                            fill: true,
                            tension: 0.35,
                            pointBackgroundColor: '#ca8a04',
                            pointRadius: 5
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: { beginAtZero: true, grid: { color: '#f1f5f9' } },
                            x: { grid: { display: false } }
                        },
                        plugins: { legend: { display: false } }
                    }
                });
            }
        })
        .catch(err => console.error("Error loading dashboard chart data", err));
}

function escapeHtml(text) {
    if (!text) return '';
    return text.toString()
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
