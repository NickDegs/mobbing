import Foundation
import StoreKit

/// Rüşvet IAP — consumable, com.nickdegs.mobbing.bribe
@MainActor
final class BribeStore: ObservableObject {
    static let productId = "com.nickdegs.mobbing.bribe"

    @Published var product: Product?
    @Published var purchasing = false

    init() {
        Task { await load() }
    }

    func load() async {
        product = try? await Product.products(for: [Self.productId]).first
    }

    /// true = satın alma başarılı (rüşvet kabul edildi)
    func buy() async -> Bool {
        guard let product else { return false }
        purchasing = true
        defer { purchasing = false }
        guard let result = try? await product.purchase() else { return false }
        switch result {
        case .success(let verification):
            if case .verified(let tx) = verification {
                await tx.finish()
                return true
            }
            return false
        default:
            return false
        }
    }

    var priceLabel: String { product?.displayPrice ?? "$0.99" }
}
