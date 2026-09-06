import AVFoundation
import SwiftUI
import UIKit

/// La vue qui porte la couche d'aperçu, sans rien d'autre.
final class VueCamera: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var couche: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
}

/**
 L'aperçu de la caméra.

 `resizeAspect` plutôt que `resizeAspectFill` : le cadre qui accueille cette
 vue adopte déjà les proportions exactes de l'image, si bien qu'il n'y a ni
 boîte aux lettres ni recadrage — et le point touché désigne donc bien le
 pixel visé, ce dont dépend toute la justesse du spot.
 */
struct VueApercu: UIViewRepresentable {

    let session: AVCaptureSession

    func makeUIView(context: Context) -> VueCamera {
        let vue = VueCamera()
        vue.backgroundColor = .black
        vue.couche.session = session
        vue.couche.videoGravity = .resizeAspect
        orienter(vue)
        return vue
    }

    func updateUIView(_ vue: VueCamera, context: Context) {
        if vue.couche.session !== session { vue.couche.session = session }
        orienter(vue)
    }

    /// L'application est verrouillée en portrait : l'aperçu l'est aussi.
    private func orienter(_ vue: VueCamera) {
        guard let lien = vue.couche.connection else { return }
        if #available(iOS 17.0, *) {
            if lien.isVideoRotationAngleSupported(90) {
                lien.videoRotationAngle = 90
            }
        } else {
            if lien.isVideoOrientationSupported {
                lien.videoOrientation = .portrait
            }
        }
    }
}
