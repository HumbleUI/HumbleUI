{
  description = "Clojure Desktop UI framework ";

  inputs = {
    nixpkgs.url     = github:nixos/nixpkgs/release-22.05;
    flake-utils.url = github:numtide/flake-utils;

    # Used for shell.nix
    flake-compat = {
      url = github:edolstra/flake-compat;
      flake = false;
    };
  };

  outputs = {
    self,
      nixpkgs,
      flake-utils,
      ...
  } @ inputs: let
    overlays = [
      # Other overlays
      (final: prev: {
      })
    ];

  in
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = import nixpkgs {inherit overlays system;};
      in rec {
        devShells.default = pkgs.mkShell {
          nativeBuildInputs = with pkgs; [
            jdk11
            clojure
          ];

          buildInputs = with pkgs; [
            # TODO: `HumbleUI` currently uses XWayland; would love for it to be Wayland-native.
            # This is probably an issue with the underlying C++ dependency, and explains the DPI issues -
            # but not the lack of focus on the text box.
            wayland
            wayland-protocols
            wlroots

            python3
            libGL
          ];

          LD_LIBRARY_PATH = "${pkgs.lib.makeLibraryPath (with pkgs; [ libGL ])}:$LD_LIBRARY_PATH";
        };

        # For compatibility with older versions of the `nix` binary
        devShell = self.devShells.${system}.default;
      }
    );
}
